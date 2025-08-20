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

package io.hotmoka.helpers.internal;

import static java.math.BigInteger.ZERO;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import io.hotmoka.helpers.AbstractNodeDecorator;
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
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;

/**
 * Shared implementation of a node where the jar with the basic Takamaka classes have been installed,
 * along with a gamete and a manifest.
 */
public abstract class InitializedNodeImpl<N extends Node, C extends ConsensusConfig<?,?>> extends AbstractNodeDecorator<N> implements InitializedNode {

	/**
	 * The storage reference of the gamete that has been generated.
	 */
	private final StorageReference gamete;

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @throws TransactionRejectedException if some transaction gets rejected
	 * @throws TransactionException if some transaction fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 * @throws ClosedNodeException if the node is already closed
	 * @throws UnexpectedCodeException if the Takamaka code in the store of the node is unexpected
	 */
	public InitializedNodeImpl(N parent, C consensus, Path takamakaCode)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, TimeoutException, InterruptedException, ClosedNodeException, UnexpectedCodeException {

		super(parent);

		// we install the jar containing the basic Takamaka classes
		TransactionReference takamakaCodeReference = parent.addJarStoreInitialTransaction(TransactionRequests.jarStoreInitial(Files.readAllBytes(takamakaCode)));

		// we create a gamete
		this.gamete = parent.addGameteCreationTransaction(TransactionRequests.gameteCreation(takamakaCodeReference, consensus.getInitialSupply(), consensus.getPublicKeyOfGameteBase64()));

		// we create the builder of the validators
		StorageReference builderOfValidators = mkValidatorsBuilder(consensus, takamakaCodeReference);

		// we create the builder of the gas station
		StorageReference builderOfGasStation = mkGasStationBuilder(consensus, takamakaCodeReference);

		BigInteger nonceOfGamete = getNonceOfGamete(takamakaCodeReference);
		var function = StorageTypes.fromClass(Function.class);

		// we create the manifest, passing the storage array of validators in store and their powers
		var request = TransactionRequests.constructorCall
			(new byte[0], gamete, nonceOfGamete, "", BigInteger.valueOf(1_000_000), ZERO, takamakaCodeReference,
					ConstructorSignatures.of(StorageTypes.MANIFEST, StorageTypes.STRING, StorageTypes.STRING,
					StorageTypes.INT, StorageTypes.LONG, StorageTypes.LONG, StorageTypes.BOOLEAN, StorageTypes.BOOLEAN,
					StorageTypes.STRING, StorageTypes.GAMETE, StorageTypes.LONG, function, function),
					StorageValues.stringOf(consensus.getGenesisTime().toInstant(ZoneOffset.UTC).toString()),
					StorageValues.stringOf(consensus.getChainId()), StorageValues.intOf(consensus.getMaxDependencies()),
					StorageValues.longOf(consensus.getMaxCumulativeSizeOfDependencies()),
					StorageValues.longOf(consensus.getMaxRequestSize()),
					StorageValues.booleanOf(consensus.allowsUnsignedFaucet()), StorageValues.booleanOf(consensus.skipsVerification()),
					StorageValues.stringOf(consensus.getSignatureForRequests().getName()), gamete, StorageValues.longOf(consensus.getVerificationVersion()),
					builderOfValidators, builderOfGasStation);

		StorageReference manifest = parent.addConstructorCallTransaction(request);

		// we install the manifest and initialize the node
		parent.addInitializationTransaction(TransactionRequests.initialization(takamakaCodeReference, manifest));
	}

	/**
	 * Yields an algorithm that creates the builder of the gas station to be installed in the manifest of the node.
	 * 
	 * @param consensus the consensus of the node being initialized
	 * @param takamakaCode the reference to the Takamaka code of the node being initialized
	 * @return the algorithm, as a builder object of the gas station
	 * @throws TransactionRejectedException if some transaction gets rejected
	 * @throws TransactionException if some transaction fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 * @throws ClosedNodeException if the node is already closed
	 * @throws UnexpectedCodeException if the Takamaka code in the store of the node is unexpected
	 */
	protected StorageReference mkGasStationBuilder(C consensus, TransactionReference takamakaCode) throws TransactionRejectedException, TransactionException, CodeExecutionException, ClosedNodeException, UnexpectedCodeException, TimeoutException, InterruptedException {
		BigInteger nonceOfGamete = getNonceOfGamete(takamakaCode);

		// we create the builder of a generic gas station
		var request = TransactionRequests.constructorCall
				(new byte[0], gamete, nonceOfGamete, "", BigInteger.valueOf(200_000), ZERO, takamakaCode,
						ConstructorSignatures.of(StorageTypes.classNamed("io.takamaka.code.governance.GenericGasStation$Builder"),
								StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.BOOLEAN, StorageTypes.BIG_INTEGER, StorageTypes.LONG),
						StorageValues.bigIntegerOf(consensus.getInitialGasPrice()), StorageValues.bigIntegerOf(consensus.getMaxGasPerTransaction()),
						StorageValues.booleanOf(consensus.ignoresGasPrice()), StorageValues.bigIntegerOf(consensus.getTargetGasAtReward()),
						StorageValues.longOf(consensus.getOblivion()));

		return addConstructorCallTransaction(request);
	}

	/**
	 * Yields an algorithm that creates the builder of the validators to be installed in the manifest of the node.
	 * 
	 * @param consensus the consensus of the node being initialized
	 * @param takamakaCode the reference to the Takamaka code of the node being initialized
	 * @return the algorithm, as a builder object of the validators object
	 * @throws TransactionRejectedException if some transaction gets rejected
	 * @throws TransactionException if some transaction fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 * @throws ClosedNodeException if the node is already closed
	 * @throws UnexpectedCodeException if the Takamaka code in the store of the node is unexpected
	 */
	protected abstract StorageReference mkValidatorsBuilder(C consensus, TransactionReference takamakaCode) throws TransactionRejectedException, TransactionException, CodeExecutionException, ClosedNodeException, UnexpectedCodeException, TimeoutException, InterruptedException;

	/**
	 * Yields the nonce of the gamete object.
	 * 
	 * @param takamakaCode the reference to the Takamaka code of the node being initialized
	 * @return the nonce of the gamete object
	 * @throws TransactionRejectedException if some transaction gets rejected
	 * @throws TransactionException if some transaction fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 * @throws ClosedNodeException if the node is already closed
	 * @throws UnexpectedCodeException if the Takamaka code in the store of the node is unexpected
	 */
	protected BigInteger getNonceOfGamete(TransactionReference takamakaCode) throws ClosedNodeException, TimeoutException, InterruptedException, TransactionRejectedException, TransactionException, UnexpectedCodeException, CodeExecutionException {
		InstanceMethodCallTransactionRequest request;
		request = TransactionRequests.instanceViewMethodCall(gamete, BigInteger.valueOf(100_000), takamakaCode, MethodSignatures.NONCE, gamete);

		return runInstanceMethodCallTransaction(request)
				.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.NONCE))
				.asReturnedBigInteger(MethodSignatures.NONCE, UnexpectedValueException::new);
	}

	@Override
	public StorageReference gamete() throws ClosedNodeException {
		ensureNotClosed();
		return gamete;
	}
}