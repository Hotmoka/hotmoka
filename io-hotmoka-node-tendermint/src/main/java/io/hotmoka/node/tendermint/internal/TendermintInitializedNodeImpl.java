/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.tendermint.internal;

import static java.math.BigInteger.ZERO;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import io.hotmoka.crypto.Base64;
import io.hotmoka.helpers.AbstractNodeDecorator;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.helpers.InitializedNodes.ProducerOfStorageObject;
import io.hotmoka.helpers.UnexpectedValueException;
import io.hotmoka.helpers.UnexpectedVoidMethodException;
import io.hotmoka.helpers.api.InitializedNode;
import io.hotmoka.helpers.api.UnexpectedCodeException;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.TendermintConsensusConfig;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.tendermint.api.TendermintInitializedNode;
import io.hotmoka.node.tendermint.api.TendermintNode;

/**
 * A decorator of a node, that installs a jar and creates some initial accounts in it.
 * Compared to the {@link io.hotmoka.helpers.api.InitializedNode} interface, this
 * class feeds the initialized node with the chain identifier and the
 * validators set of the underlying Tendermint network.
 */
public class TendermintInitializedNodeImpl extends AbstractNodeDecorator<InitializedNode> implements TendermintInitializedNode {

	private final static Logger LOGGER = Logger.getLogger(TendermintInitializedNodeImpl.class.getName());

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given keys to control the gamete. Uses the chain id, the genesis time and the validators
	 * of the underlying Tendermint engine. It uses a generic gas station.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 * @throws UnexpectedCodeException if the Takamaka runtime installed in the node contains unexpected code
	 * @throws ClosedNodeException if the node is already closed
	 */
	public TendermintInitializedNodeImpl(TendermintNode parent, TendermintConsensusConfig<?,?> consensus, Path takamakaCode)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, TimeoutException, InterruptedException, ClosedNodeException, UnexpectedCodeException {

		super(mkParent(parent, consensus, null, takamakaCode));
	}

	private static InitializedNode mkParent(TendermintNode parent, TendermintConsensusConfig<?,?> consensus, ProducerOfStorageObject producerOfGasStationBuilder, Path takamakaCode) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, TimeoutException, InterruptedException, ClosedNodeException, UnexpectedCodeException {
		var tendermintConfigFile = new TendermintConfigFile(parent.getLocalConfig());
		var poster = new TendermintPoster(parent.getLocalConfig(), tendermintConfigFile.getTendermintPort());

		// we modify the consensus parameters, by setting the chain identifier and the genesis time to that of the underlying Tendermint network
		final var consensus2 = consensus.toBuilder()
			.setChainId(poster.getTendermintChainId())
			.setGenesisTime(poster.getGenesisTime())
			.build();

		return InitializedNodes.of(parent, consensus, takamakaCode,
			(node, takamakaCodeReference) -> createTendermintValidatorsBuilder(poster, node, consensus2, takamakaCodeReference),
			producerOfGasStationBuilder);
	}

	private static StorageReference createTendermintValidatorsBuilder(TendermintPoster poster, InitializedNode node, TendermintConsensusConfig<?,?> consensus, TransactionReference takamakaCodeReference)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, ClosedNodeException, UnexpectedCodeException, TimeoutException, InterruptedException {

		StorageReference gamete = node.gamete();
		var getNonceRequest = TransactionRequests.instanceViewMethodCall(gamete, BigInteger.valueOf(50_000), takamakaCodeReference, MethodSignatures.NONCE, gamete);
		BigInteger nonce = node.runInstanceMethodCallTransaction(getNonceRequest)
			.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.NONCE))
			.asReturnedBigInteger(MethodSignatures.NONCE, UnexpectedValueException::new);

		// we create validators corresponding to those declared in the configuration file of the Tendermint node
		TendermintValidator[] tendermintValidators = poster.getTendermintValidators();

		// we create the builder of the validators
		var _200_000 = BigInteger.valueOf(200_000);
		ClassType builderClass = StorageTypes.classNamed(StorageTypes.TENDERMINT_VALIDATORS + "$Builder");

		var request = TransactionRequests.constructorCall
			(new byte[0], gamete, nonce, "", _200_000, ZERO, takamakaCodeReference,
				ConstructorSignatures.of(builderClass, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER,
					StorageTypes.INT, StorageTypes.INT, StorageTypes.INT, StorageTypes.INT),
					StorageValues.bigIntegerOf(consensus.getTicketForNewPoll()), StorageValues.bigIntegerOf(consensus.getFinalSupply()),
					StorageValues.bigIntegerOf(consensus.getHeightAtFinalSupply()),
					StorageValues.intOf(consensus.getPercentStaked()), StorageValues.intOf(consensus.getBuyerSurcharge()),
					StorageValues.intOf(consensus.getSlashingForMisbehaving()), StorageValues.intOf(consensus.getSlashingForNotBehaving()));

		StorageReference builder = node.addConstructorCallTransaction(request);
		nonce = nonce.add(BigInteger.ONE);

		// we populate the builder with a Tendermint validator at a time; this guarantees that they are created with 0 as progressive identifier 
		var addValidatorMethod = MethodSignatures.ofVoid(builderClass, "addValidator", StorageTypes.STRING, StorageTypes.LONG);

		for (TendermintValidator tv: tendermintValidators) {
			String publicKeyBase64 = Base64.toBase64String(tv.getPubliKeyEncoded());
			var addValidator = TransactionRequests.instanceMethodCall
				(new byte[0], gamete, nonce, "", _200_000, ZERO, takamakaCodeReference,
				addValidatorMethod, builder, StorageValues.stringOf(publicKeyBase64), StorageValues.longOf(tv.power));
			node.addInstanceMethodCallTransaction(addValidator);
			LOGGER.info("added Tendermint validator with address " + tv.address + " and power " + tv.power);
			nonce = nonce.add(BigInteger.ONE);
		}

		return builder;
	}

	@Override
	public StorageReference gamete() throws ClosedNodeException, TimeoutException, InterruptedException {
		ensureNotClosed();
		return getParent().gamete();
	}
}