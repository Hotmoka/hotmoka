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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.helpers.AbstractNodeDecorator;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.helpers.InitializedNodes.ProducerOfStorageObject;
import io.hotmoka.helpers.api.InitializedNode;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.nodes.ValidatorsConsensusConfig;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.tendermint.api.TendermintNode;

/**
 * A decorator of a node, that installs a jar and creates some initial accounts in it.
 * Compared to the {@link io.hotmoka.helpers.api.views.InitializedNode} interface, this
 * class feeds the initialized node with the chain identifier and the
 * validators set of the underlying Tendermint network.
 */
public class TendermintInitializedNodeImpl extends AbstractNodeDecorator<InitializedNode> implements InitializedNode {

	private final static Logger LOGGER = Logger.getLogger(TendermintInitializedNodeImpl.class.getName());

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given keys to control the gamete. Uses the chain id, the genesis time and the validators
	 * of the underlying Tendermint network. It allows to specify the gas station to use.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param producerOfGasStationBuilder
	 * 		an algorithm that creates the builder of the gas station to be installed in the manifest of the node;
	 *      if this is {@code null}, a generic gas station is created
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	public TendermintInitializedNodeImpl(TendermintNode parent, ValidatorsConsensusConfig<?,?> consensus, Path takamakaCode)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, NodeException, TimeoutException, InterruptedException {

		super(mkParent(parent, consensus, takamakaCode));
	}

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given keys to control the gamete. Uses the chain id, the genesis time and the validators
	 * of the underlying Tendermint network. It allows to specify the gas station to use.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param producerOfGasStationBuilder
	 * 		an algorithm that creates the builder of the gas station to be installed in the manifest of the node;
	 *      if this is {@code null}, a generic gas station is created
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	public TendermintInitializedNodeImpl(TendermintNode parent, ValidatorsConsensusConfig<?,?> consensus, ProducerOfStorageObject<ConsensusConfig<?,?>> producerOfGasStationBuilder, Path takamakaCode)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, NodeException, TimeoutException, InterruptedException {

		super(mkParent(parent, consensus, producerOfGasStationBuilder, takamakaCode));
	}

	private static InitializedNode mkParent(TendermintNode parent, ValidatorsConsensusConfig<?,?> consensus, Path takamakaCode) throws NodeException, TransactionRejectedException, TransactionException, CodeExecutionException, IOException, TimeoutException, InterruptedException {
		var tendermintConfigFile = new TendermintConfigFile(parent.getLocalConfig());
		var poster = new TendermintPoster(parent.getLocalConfig(), tendermintConfigFile.tendermintPort);

		// we modify the consensus parameters, by setting the chain identifier and the genesis time to that of the underlying Tendermint network
		consensus = consensus.toBuilder()
			.setChainId(poster.getTendermintChainId())
			.setGenesisTime(LocalDateTime.parse(poster.getGenesisTime(), DateTimeFormatter.ISO_DATE_TIME))
			.build();

		return InitializedNodes.of(parent, consensus, takamakaCode,
			(node, _consensus, takamakaCodeReference) -> createTendermintValidatorsBuilder(poster, node, _consensus, takamakaCodeReference),
			null);
	}

	private static InitializedNode mkParent(TendermintNode parent, ValidatorsConsensusConfig<?,?> consensus, ProducerOfStorageObject<ConsensusConfig<?,?>> producerOfGasStationBuilder, Path takamakaCode) throws NodeException, TransactionRejectedException, TransactionException, CodeExecutionException, IOException, TimeoutException, InterruptedException {
		var tendermintConfigFile = new TendermintConfigFile(parent.getLocalConfig());
		var poster = new TendermintPoster(parent.getLocalConfig(), tendermintConfigFile.tendermintPort);

		// we modify the consensus parameters, by setting the chain identifier and the genesis time to that of the underlying Tendermint network
		consensus = consensus.toBuilder()
			.setChainId(poster.getTendermintChainId())
			.setGenesisTime(LocalDateTime.parse(poster.getGenesisTime(), DateTimeFormatter.ISO_DATE_TIME))
			.build();

		return InitializedNodes.of(parent, consensus, takamakaCode,
			(node, _consensus, takamakaCodeReference) -> createTendermintValidatorsBuilder(poster, node, _consensus, takamakaCodeReference),
			producerOfGasStationBuilder);
	}

	private static StorageReference createTendermintValidatorsBuilder(TendermintPoster poster, InitializedNode node, ValidatorsConsensusConfig<?,?> consensus, TransactionReference takamakaCodeReference)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {

		StorageReference gamete = node.gamete();
		var getNonceRequest = TransactionRequests.instanceViewMethodCall(gamete, BigInteger.valueOf(50_000), takamakaCodeReference, MethodSignatures.NONCE, gamete);
		BigInteger nonceOfGamete;

		try {
			nonceOfGamete = node.runInstanceMethodCallTransaction(getNonceRequest)
				.orElseThrow(() -> new NodeException(MethodSignatures.NONCE + " should not return void"))
				.asBigInteger(value -> new NodeException(MethodSignatures.NONCE + " should return a BigInteger, not a " + value.getClass().getName()));
		}
		catch (CodeExecutionException | TransactionException e) {
			// this run call cannot fail for the gamete, unless the node is corrupted
			throw new NodeException(e);
		}

		// we create validators corresponding to those declared in the configuration file of the Tendermint node
		var tendermintValidators = poster.getTendermintValidators().toArray(TendermintValidator[]::new);

		// we create the builder of the validators
		var _200_000 = BigInteger.valueOf(200_000);
		ClassType builderClass = StorageTypes.classNamed(StorageTypes.TENDERMINT_VALIDATORS + "$Builder", IllegalArgumentException::new);

		var request = TransactionRequests.constructorCall
			(new byte[0], gamete, nonceOfGamete, "", _200_000, ZERO, takamakaCodeReference,
					ConstructorSignatures.of(builderClass, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.LONG,
					StorageTypes.INT, StorageTypes.INT, StorageTypes.INT, StorageTypes.INT),
			new StorageValue[] {
				StorageValues.bigIntegerOf(consensus.getTicketForNewPoll()), StorageValues.bigIntegerOf(consensus.getFinalSupply()),
				StorageValues.longOf(consensus.getInitialInflation()), StorageValues.intOf(consensus.getPercentStaked()), StorageValues.intOf(consensus.getBuyerSurcharge()),
				StorageValues.intOf(consensus.getSlashingForMisbehaving()), StorageValues.intOf(consensus.getSlashingForNotBehaving())
			},
			IllegalArgumentException::new);

		nonceOfGamete = nonceOfGamete.add(BigInteger.ONE);

		StorageReference builder = node.addConstructorCallTransaction(request);

		// we populate the builder with a Tendermint validator at a time; this guarantees that they are created with 0 as progressive identifier 
		var addValidatorMethod = MethodSignatures.ofVoid(builderClass, "addValidator", StorageTypes.STRING, StorageTypes.LONG);
		for (TendermintValidator tv: tendermintValidators) {
			String publicKeyBase64;

			try {
				var ed25519 = SignatureAlgorithms.ed25519();
				publicKeyBase64 = Base64.toBase64String(ed25519.encodingOf(publicKeyFromTendermintValidator(tv, ed25519)));
			}
			catch (NoSuchAlgorithmException e) {
				throw new NodeException(e);
			}
			catch (InvalidKeyException e) {
				throw new NodeException("Tendermint answered with an illegal key", e);
			}

			long power = powerFromTendermintValidator(tv);
			var addValidator = TransactionRequests.instanceMethodCall
				(new byte[0], gamete, nonceOfGamete, "", _200_000, ZERO, takamakaCodeReference,
				addValidatorMethod, builder, new StorageValue[] { StorageValues.stringOf(publicKeyBase64), StorageValues.longOf(power) }, IllegalArgumentException::new);
			node.addInstanceMethodCallTransaction(addValidator);
			nonceOfGamete = nonceOfGamete.add(BigInteger.ONE);
		}

		Stream.of(tendermintValidators)
			.forEachOrdered(tv -> LOGGER.info("added Tendermint validator with address " + tv.address + " and power " + tv.power));

		return builder;
	}

	private static PublicKey publicKeyFromTendermintValidator(TendermintValidator validator, SignatureAlgorithm ed25519) throws NodeException {
		if (!"tendermint/PubKeyEd25519".equals(validator.publicKeyType))
			throw new NodeException("It is currently possible to create Tendermint validators only if they use Ed25519 keys");

        try {
        	byte[] encoded = Base64.fromBase64String(validator.publicKey); // TODO: can we probably return validator.publicKey directly?
        	return ed25519.publicKeyFromEncoding(encoded);
		}
        catch (InvalidKeySpecException | Base64ConversionException e) {
        	throw new NodeException("Tendermint answered with an illegal key", e);
		}
	}

	private static long powerFromTendermintValidator(TendermintValidator validator) throws NodeException {
		if (!"tendermint/PubKeyEd25519".equals(validator.publicKeyType))
			throw new NodeException("It is currently possible to create Tendermint validators only if they use Ed25519 keys");

		return validator.power;
	}

	@Override
	public StorageReference gamete() throws NodeException, TimeoutException, InterruptedException {
		ensureNotClosed();
		return getParent().gamete();
	}
}