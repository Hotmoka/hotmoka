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
import io.hotmoka.helpers.InitializedNodes.ProducerOfStorageObject;
import io.hotmoka.helpers.api.InitializedNode;
import io.hotmoka.node.ClosedNodeException;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.nodes.ValidatorsConsensusConfig;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;

/**
 * A decorator of a node, that installs a jar and creates some initial accounts in it.
 * It is mainly useful for testing.
 */
public class InitializedNodeImpl extends AbstractNodeDecorator<Node> implements InitializedNode {

	/**
	 * The storage reference of the gamete that has been generated.
	 */
	private final StorageReference gamete;

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given key pair for controlling the gamete.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @param producerOfValidatorsBuilder an algorithm that creates the builder of the validators to be installed in the manifest of the node;
	 *                                    if this is {@code null}, a generic empty set of validators is created
	 * @param producerOfGasStationBuilder an algorithm that creates the builder of the gas station to be installed in the manifest of the node;
	 *                                    if this is {@code null}, a generic gas station is created
	 * @throws TransactionRejectedException if some transaction gets rejected
	 * @throws TransactionException if some transaction fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	public InitializedNodeImpl(Node parent, ValidatorsConsensusConfig<?,?> consensus, Path takamakaCode,
			ProducerOfStorageObject<ValidatorsConsensusConfig<?,?>> producerOfValidatorsBuilder, ProducerOfStorageObject<ConsensusConfig<?,?>> producerOfGasStationBuilder)
				throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, NodeException, TimeoutException, InterruptedException {

		super(parent);

		// we install the jar containing the basic Takamaka classes
		TransactionReference takamakaCodeReference = parent.addJarStoreInitialTransaction(TransactionRequests.jarStoreInitial(Files.readAllBytes(takamakaCode), new TransactionReference[0], IllegalArgumentException::new));

		// we create a gamete with both red and green coins
		this.gamete = parent.addGameteCreationTransaction(TransactionRequests.gameteCreation(takamakaCodeReference, consensus.getInitialSupply(), consensus.getPublicKeyOfGameteBase64(), IllegalArgumentException::new));

		if (producerOfValidatorsBuilder == null)
			producerOfValidatorsBuilder = this::createEmptyValidatorsBuilder;

		if (producerOfGasStationBuilder == null)
			producerOfGasStationBuilder = this::createGenericGasStationBuilder;

		// we create the builder of the validators
		StorageReference builderOfValidators = producerOfValidatorsBuilder.apply(this, consensus, takamakaCodeReference);

		// we create the builder of the gas station
		StorageReference builderOfGasStation = producerOfGasStationBuilder.apply(this, consensus, takamakaCodeReference);

		BigInteger nonceOfGamete = getNonceOfGamete(parent, takamakaCodeReference);
		var function = StorageTypes.fromClass(Function.class, IllegalArgumentException::new);

		// we create the manifest, passing the storage array of validators in store and their powers
		var request = TransactionRequests.constructorCall
			(new byte[0], gamete, nonceOfGamete, "", BigInteger.valueOf(1_000_000), ZERO, takamakaCodeReference,
					ConstructorSignatures.of(StorageTypes.MANIFEST, StorageTypes.STRING, StorageTypes.STRING, StorageTypes.INT,
					StorageTypes.INT, StorageTypes.LONG,
					StorageTypes.BOOLEAN, StorageTypes.BOOLEAN,
					StorageTypes.STRING, StorageTypes.GAMETE, StorageTypes.LONG, function, function),
			new StorageValue[] {
				StorageValues.stringOf(consensus.getGenesisTime().toInstant(ZoneOffset.UTC).toString()),
				StorageValues.stringOf(consensus.getChainId()), StorageValues.intOf(consensus.getMaxErrorLength()), StorageValues.intOf(consensus.getMaxDependencies()),
				StorageValues.longOf(consensus.getMaxCumulativeSizeOfDependencies()),
				StorageValues.booleanOf(consensus.allowsUnsignedFaucet()), StorageValues.booleanOf(consensus.skipsVerification()),
				StorageValues.stringOf(consensus.getSignatureForRequests().getName()), gamete, StorageValues.longOf(consensus.getVerificationVersion()),
				builderOfValidators, builderOfGasStation
			},
			IllegalArgumentException::new);

		StorageReference manifest = parent.addConstructorCallTransaction(request);

		// we install the manifest and initialize the node
		parent.addInitializationTransaction(TransactionRequests.initialization(takamakaCodeReference, manifest, NodeException::new));
	}

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given key pair for controlling the gamete. It installs empty validators.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @throws TransactionRejectedException if some transaction gets rejected
	 * @throws TransactionException if some transaction fails
	 * @throws IOException if the jar file cannot be accessed
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 * @throws TransactionException if some transaction fails
	 */
	public InitializedNodeImpl(Node parent, ConsensusConfig<?,?> consensus, Path takamakaCode) throws TransactionRejectedException, TransactionException, IOException, NodeException, TimeoutException, InterruptedException {
		super(parent);

		// we install the jar containing the basic Takamaka classes
		TransactionReference takamakaCodeReference = parent.addJarStoreInitialTransaction(TransactionRequests.jarStoreInitial(Files.readAllBytes(takamakaCode), new TransactionReference[0], IllegalArgumentException::new));

		// we create a gamete with both red and green coins
		this.gamete = parent.addGameteCreationTransaction(TransactionRequests.gameteCreation(takamakaCodeReference, consensus.getInitialSupply(), consensus.getPublicKeyOfGameteBase64(), IllegalArgumentException::new));

		// we create the builder of the validators
		StorageReference builderOfValidators = createEmptyValidatorsBuilder(this, consensus, takamakaCodeReference);

		// we create the builder of the gas station
		StorageReference builderOfGasStation = createGenericGasStationBuilder(this, consensus, takamakaCodeReference);

		BigInteger nonceOfGamete = getNonceOfGamete(parent, takamakaCodeReference);
		var function = StorageTypes.fromClass(Function.class, IllegalArgumentException::new);

		// we create the manifest, passing the storage array of validators in store and their powers
		var request = TransactionRequests.constructorCall
			(new byte[0], gamete, nonceOfGamete, "", BigInteger.valueOf(1_000_000), ZERO, takamakaCodeReference,
					ConstructorSignatures.of(StorageTypes.MANIFEST, StorageTypes.STRING, StorageTypes.STRING, StorageTypes.INT,
					StorageTypes.INT, StorageTypes.LONG,
					StorageTypes.BOOLEAN, StorageTypes.BOOLEAN,
					StorageTypes.STRING, StorageTypes.GAMETE, StorageTypes.LONG, function, function),
			new StorageValue[] {
				StorageValues.stringOf(consensus.getGenesisTime().toString()),
				StorageValues.stringOf(consensus.getChainId()), StorageValues.intOf(consensus.getMaxErrorLength()), StorageValues.intOf(consensus.getMaxDependencies()),
				StorageValues.longOf(consensus.getMaxCumulativeSizeOfDependencies()),
				StorageValues.booleanOf(consensus.allowsUnsignedFaucet()), StorageValues.booleanOf(consensus.skipsVerification()),
				StorageValues.stringOf(consensus.getSignatureForRequests().getName()), gamete, StorageValues.longOf(consensus.getVerificationVersion()),
				builderOfValidators, builderOfGasStation
			},
			IllegalArgumentException::new);

		StorageReference manifest;

		try {
			manifest = parent.addConstructorCallTransaction(request);
		}
		catch (CodeExecutionException e) {
			// the called method does not throw exceptions
			throw new NodeException(e);
		}

		// we install the manifest and initialize the node
		parent.addInitializationTransaction(TransactionRequests.initialization(takamakaCodeReference, manifest, NodeException::new));
	}

	private BigInteger getNonceOfGamete(Node node, TransactionReference takamakaCode) throws NodeException, TimeoutException, InterruptedException, TransactionRejectedException, TransactionException {
		var _1_000_000 = BigInteger.valueOf(1_000_000);
		var getNonceRequest = TransactionRequests.instanceViewMethodCall(gamete, _1_000_000, takamakaCode, MethodSignatures.NONCE, gamete, StorageValues.NO_VALUES, IllegalArgumentException::new);

		try {
			return node.runInstanceMethodCallTransaction(getNonceRequest)
				.orElseThrow(() -> new NodeException(MethodSignatures.NONCE + " should not return void"))
				.asReturnedBigInteger(MethodSignatures.NONCE, NodeException::new);
		}
		catch (CodeExecutionException e) {
			// the nonce() method does not throw exceptions
			throw new NodeException(e);
		}
	}

	private StorageReference createEmptyValidatorsBuilder(InitializedNode node, ConsensusConfig<?,?> consensus, TransactionReference takamakaCode) throws NodeException, TimeoutException, InterruptedException, TransactionRejectedException, TransactionException {
		var _200_000 = BigInteger.valueOf(200_000);
		var nonceOfGamete = getNonceOfGamete(node, takamakaCode);
	
		try {
			// we create the builder of zero validators
			var request = TransactionRequests.constructorCall
					(new byte[0], gamete, nonceOfGamete, "", _200_000, ZERO, takamakaCode,
							ConstructorSignatures.of(StorageTypes.classNamed("io.takamaka.code.governance.GenericValidators$Builder", IllegalArgumentException::new), StorageTypes.STRING,
									StorageTypes.STRING, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.LONG,
									StorageTypes.INT, StorageTypes.INT, StorageTypes.INT, StorageTypes.INT),
					new StorageValue[] {
							StorageValues.stringOf(""), StorageValues.stringOf(""), StorageValues.bigIntegerOf(consensus.getTicketForNewPoll()), StorageValues.bigIntegerOf(consensus.getFinalSupply()),
							StorageValues.longOf(consensus.getInitialInflation()), StorageValues.intOf(0),
							StorageValues.intOf(0), StorageValues.intOf(0), StorageValues.intOf(0)
					},
					IllegalArgumentException::new);

			return node.addConstructorCallTransaction(request);
		}
		catch (CodeExecutionException e) {
			// the called constructor does not throw exceptions
			throw new NodeException(e);
		}
	}

	private StorageReference createGenericGasStationBuilder(InitializedNode node, ConsensusConfig<?,?> consensus, TransactionReference takamakaCode) throws NodeException, TimeoutException, InterruptedException, TransactionRejectedException, TransactionException {
		BigInteger nonceOfGamete = getNonceOfGamete(node, takamakaCode);
	
		try {
			// we create the builder of a generic gas station
			var request = TransactionRequests.constructorCall
					(new byte[0], gamete, nonceOfGamete, "", BigInteger.valueOf(100_000), ZERO, takamakaCode,
							ConstructorSignatures.of(StorageTypes.classNamed("io.takamaka.code.governance.GenericGasStation$Builder", IllegalArgumentException::new),
									StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.BOOLEAN, StorageTypes.BIG_INTEGER, StorageTypes.LONG),
					new StorageValue[] {
						StorageValues.bigIntegerOf(consensus.getInitialGasPrice()), StorageValues.bigIntegerOf(consensus.getMaxGasPerTransaction()),
						StorageValues.booleanOf(consensus.ignoresGasPrice()), StorageValues.bigIntegerOf(consensus.getTargetGasAtReward()),
						StorageValues.longOf(consensus.getOblivion())
					},
					IllegalArgumentException::new);

			return node.addConstructorCallTransaction(request);
		}
		catch (CodeExecutionException e) {
			// the called constructor does not throw exceptions
			throw new NodeException(e);
		}
	}

	@Override
	public StorageReference gamete() throws ClosedNodeException {
		ensureNotClosed();
		return gamete;
	}
}