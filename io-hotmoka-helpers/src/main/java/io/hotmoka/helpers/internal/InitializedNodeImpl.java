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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionRequests;
import io.hotmoka.beans.api.nodes.NodeInfo;
import io.hotmoka.beans.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.api.requests.InitializationTransactionRequest;
import io.hotmoka.beans.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.api.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.updates.ClassTag;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.closeables.api.OnCloseHandler;
import io.hotmoka.helpers.InitializedNodes.ProducerOfStorageObject;
import io.hotmoka.helpers.api.InitializedNode;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.CodeSupplier;
import io.hotmoka.node.api.ConsensusConfig;
import io.hotmoka.node.api.JarSupplier;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.ValidatorsConsensusConfig;

/**
 * A decorator of a node, that installs a jar and creates some initial accounts in it.
 * It is mainly useful for testing.
 */
public class InitializedNodeImpl implements InitializedNode {

	/**
	 * The node that is decorated.
	 */
	private final Node parent;

	/**
	 * The storage reference of the gamete that has been generated.
	 */
	private final StorageReference gamete;

	/**
	 * True if and only if this node has been closed already.
	 */
	private final AtomicBoolean isClosed = new AtomicBoolean();

	/**
	 * We need this intermediate definition since two instances of a method reference
	 * are not the same, nor equals.
	 */
	private final OnCloseHandler this_close = this::close;

	private StorageReference createEmptyValidatorsBuilder(InitializedNode node, ConsensusConfig<?,?> consensus, TransactionReference takamakaCodeReference) throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchAlgorithmException, NodeException, TimeoutException, InterruptedException {
		var _200_000 = BigInteger.valueOf(200_000);
		var getNonceRequest = TransactionRequests.instanceViewMethodCall
			(gamete, _200_000, takamakaCodeReference, MethodSignatures.NONCE, gamete);
		var nonceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(getNonceRequest)).getValue();

		// we create the builder of zero validators
		var request = TransactionRequests.constructorCall
			(new byte[0], gamete, nonceOfGamete, "", _200_000, ZERO, takamakaCodeReference,
					ConstructorSignatures.of("io.takamaka.code.governance.GenericValidators$Builder", StorageTypes.STRING,
					StorageTypes.STRING, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.LONG,
					StorageTypes.INT, StorageTypes.INT, StorageTypes.INT, StorageTypes.INT),
			StorageValues.stringOf(""), StorageValues.stringOf(""), StorageValues.bigIntegerOf(consensus.getTicketForNewPoll()), StorageValues.bigIntegerOf(consensus.getFinalSupply()),
			StorageValues.longOf(consensus.getInitialInflation()), StorageValues.intOf(0),
			StorageValues.intOf(0), StorageValues.intOf(0), StorageValues.intOf(0));

		return node.addConstructorCallTransaction(request);
	}

	private StorageReference createGenericGasStationBuilder(InitializedNode node, ConsensusConfig<?,?> consensus, TransactionReference takamakaCodeReference) throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchAlgorithmException, NodeException, TimeoutException, InterruptedException {
		var _100_000 = BigInteger.valueOf(100_000);
		var getNonceRequest = TransactionRequests.instanceViewMethodCall
			(gamete, _100_000, takamakaCodeReference, MethodSignatures.NONCE, gamete);
		BigInteger nonceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(getNonceRequest)).getValue();

		// we create the builder of a generic gas station
		var request = TransactionRequests.constructorCall
			(new byte[0], gamete, nonceOfGamete, "", _100_000, ZERO, takamakaCodeReference,
					ConstructorSignatures.of("io.takamaka.code.governance.GenericGasStation$Builder",
					StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.BOOLEAN, StorageTypes.BIG_INTEGER, StorageTypes.LONG),
			StorageValues.bigIntegerOf(consensus.getInitialGasPrice()), StorageValues.bigIntegerOf(consensus.getMaxGasPerTransaction()),
			StorageValues.booleanOf(consensus.ignoresGasPrice()), StorageValues.bigIntegerOf(consensus.getTargetGasAtReward()),
			StorageValues.longOf(consensus.getOblivion()));

		return node.addConstructorCallTransaction(request);
	}

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
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some initialization request could not be signed
	 * @throws InvalidKeyException if some key used for signing initialization transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	public InitializedNodeImpl(Node parent, ValidatorsConsensusConfig<?,?> consensus, Path takamakaCode,
			ProducerOfStorageObject<ValidatorsConsensusConfig<?,?>> producerOfValidatorsBuilder,
			ProducerOfStorageObject<ConsensusConfig<?,?>> producerOfGasStationBuilder) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, NodeException, TimeoutException, InterruptedException {

		this.parent = parent;

		// we install the jar containing the basic Takamaka classes
		TransactionReference takamakaCodeReference = parent.addJarStoreInitialTransaction(TransactionRequests.jarStoreInitial(Files.readAllBytes(takamakaCode)));

		// we create a gamete with both red and green coins
		this.gamete = parent.addGameteCreationTransaction(TransactionRequests.gameteCreation(takamakaCodeReference, consensus.getInitialSupply(), consensus.getInitialRedSupply(), consensus.getPublicKeyOfGamete()));

		if (producerOfValidatorsBuilder == null)
			producerOfValidatorsBuilder = this::createEmptyValidatorsBuilder;

		if (producerOfGasStationBuilder == null)
			producerOfGasStationBuilder = this::createGenericGasStationBuilder;

		// we create the builder of the validators
		StorageReference builderOfValidators = producerOfValidatorsBuilder.apply(this, consensus, takamakaCodeReference);

		// we create the builder of the gas station
		StorageReference builderOfGasStation = producerOfGasStationBuilder.apply(this, consensus, takamakaCodeReference);

		var _1_000_000 = BigInteger.valueOf(1_000_000);
		var getNonceRequest = TransactionRequests.instanceViewMethodCall
			(gamete, _1_000_000, takamakaCodeReference, MethodSignatures.NONCE, gamete);
		BigInteger nonceOfGamete = ((BigIntegerValue) parent.runInstanceMethodCallTransaction(getNonceRequest)).getValue();
		var function = StorageTypes.classNamed(Function.class.getName());

		// we create the manifest, passing the storage array of validators in store and their powers
		var request = TransactionRequests.constructorCall
			(new byte[0], gamete, nonceOfGamete, "", _1_000_000, ZERO, takamakaCodeReference,
					ConstructorSignatures.of(StorageTypes.MANIFEST, StorageTypes.STRING, StorageTypes.STRING, StorageTypes.LONG,
					StorageTypes.LONG, StorageTypes.LONG,
					StorageTypes.BOOLEAN, StorageTypes.BOOLEAN, StorageTypes.BOOLEAN,
					StorageTypes.STRING, StorageTypes.GAMETE, StorageTypes.LONG, function, function),
			StorageValues.stringOf(consensus.getGenesisTime().toInstant(ZoneOffset.UTC).toString()),
			StorageValues.stringOf(consensus.getChainId()), StorageValues.longOf(consensus.getMaxErrorLength()), StorageValues.longOf(consensus.getMaxDependencies()),
			StorageValues.longOf(consensus.getMaxCumulativeSizeOfDependencies()), StorageValues.booleanOf(consensus.allowsSelfCharged()),
			StorageValues.booleanOf(consensus.allowsUnsignedFaucet()), StorageValues.booleanOf(consensus.skipsVerification()),
			StorageValues.stringOf(consensus.getSignature().getName()), gamete, StorageValues.longOf(consensus.getVerificationVersion()),
			builderOfValidators, builderOfGasStation);

		StorageReference manifest = parent.addConstructorCallTransaction(request);

		// we install the manifest and initialize the node
		parent.addInitializationTransaction(TransactionRequests.initialization(takamakaCodeReference, manifest));

		// when the parent is closed, this decorator will be closed as well
		parent.addOnCloseHandler(this_close);
	}

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given key pair for controlling the gamete. It installs empty validators.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @param producerOfGasStationBuilder an algorithm that creates the builder of the gas station to be installed in the manifest of the node;
	 *                                    if this is {@code null}, a generic gas station is created
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some initialization request could not be signed
	 * @throws InvalidKeyException if some key used for signing initialization transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	public InitializedNodeImpl(Node parent, ConsensusConfig<?,?> consensus, Path takamakaCode,
			ProducerOfStorageObject<ConsensusConfig<?,?>> producerOfGasStationBuilder) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, NodeException, TimeoutException, InterruptedException {

		this.parent = parent;

		// we install the jar containing the basic Takamaka classes
		TransactionReference takamakaCodeReference = parent.addJarStoreInitialTransaction(TransactionRequests.jarStoreInitial(Files.readAllBytes(takamakaCode)));

		// we create a gamete with both red and green coins
		this.gamete = parent.addGameteCreationTransaction(TransactionRequests.gameteCreation(takamakaCodeReference, consensus.getInitialSupply(), consensus.getInitialRedSupply(), consensus.getPublicKeyOfGamete()));

		if (producerOfGasStationBuilder == null)
			producerOfGasStationBuilder = this::createGenericGasStationBuilder;

		// we create the builder of the validators
		StorageReference builderOfValidators = createEmptyValidatorsBuilder(this, consensus, takamakaCodeReference);

		// we create the builder of the gas station
		StorageReference builderOfGasStation = producerOfGasStationBuilder.apply(this, consensus, takamakaCodeReference);

		var _1_000_000 = BigInteger.valueOf(1_000_000);
		var getNonceRequest = TransactionRequests.instanceViewMethodCall
			(gamete, _1_000_000, takamakaCodeReference, MethodSignatures.NONCE, gamete);
		BigInteger nonceOfGamete = ((BigIntegerValue) parent.runInstanceMethodCallTransaction(getNonceRequest)).getValue();
		var function = StorageTypes.classNamed(Function.class.getName());

		// we create the manifest, passing the storage array of validators in store and their powers
		var request = TransactionRequests.constructorCall
			(new byte[0], gamete, nonceOfGamete, "", _1_000_000, ZERO, takamakaCodeReference,
					ConstructorSignatures.of(StorageTypes.MANIFEST, StorageTypes.STRING, StorageTypes.STRING, StorageTypes.LONG,
					StorageTypes.LONG, StorageTypes.LONG,
					StorageTypes.BOOLEAN, StorageTypes.BOOLEAN, StorageTypes.BOOLEAN,
					StorageTypes.STRING, StorageTypes.GAMETE, StorageTypes.LONG, function, function),
			StorageValues.stringOf(consensus.getGenesisTime().toString()),
			StorageValues.stringOf(consensus.getChainId()), StorageValues.longOf(consensus.getMaxErrorLength()), StorageValues.longOf(consensus.getMaxDependencies()),
			StorageValues.longOf(consensus.getMaxCumulativeSizeOfDependencies()), StorageValues.booleanOf(consensus.allowsSelfCharged()),
			StorageValues.booleanOf(consensus.allowsUnsignedFaucet()), StorageValues.booleanOf(consensus.skipsVerification()),
			StorageValues.stringOf(consensus.getSignature().getName()), gamete, StorageValues.longOf(consensus.getVerificationVersion()),
			builderOfValidators, builderOfGasStation);

		StorageReference manifest = parent.addConstructorCallTransaction(request);

		// we install the manifest and initialize the node
		parent.addInitializationTransaction(TransactionRequests.initialization(takamakaCodeReference, manifest));

		// when the parent is closed, this decorator will be closed as well
		parent.addOnCloseHandler(this_close);
	}

	@Override
	public StorageReference gamete() { // TODO: throw exception if closed
		return gamete;
	}

	@Override
	public void close() throws Exception {
		if (!isClosed.getAndSet(true))
			parent.close();
	}

	@Override
	public StorageReference getManifest() throws NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		return parent.getManifest();
	}

	@Override
	public TransactionReference getTakamakaCode() throws NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		return parent.getTakamakaCode();
	}

	@Override
	public NodeInfo getNodeInfo() throws NodeException, TimeoutException, InterruptedException {
		return parent.getNodeInfo();
	}

	@Override
	public ClassTag getClassTag(StorageReference reference) throws NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		return parent.getClassTag(reference);
	}

	@Override
	public Stream<Update> getState(StorageReference reference) throws NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		return parent.getState(reference);
	}

	@Override
	public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
		return parent.addJarStoreInitialTransaction(request);
	}

	@Override
	public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
		return parent.addGameteCreationTransaction(request);
	}

	@Override
	public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException {
		return parent.addJarStoreTransaction(request);
	}

	@Override
	public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.addConstructorCallTransaction(request);
	}

	@Override
	public StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.addInstanceMethodCallTransaction(request);
	}

	@Override
	public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.addStaticMethodCallTransaction(request);
	}

	@Override
	public StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return parent.runInstanceMethodCallTransaction(request);
	}

	@Override
	public StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		return parent.runStaticMethodCallTransaction(request);
	}

	@Override
	public JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
		return parent.postJarStoreTransaction(request);
	}

	@Override
	public CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
		return parent.postConstructorCallTransaction(request);
	}

	@Override
	public CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
		return parent.postInstanceMethodCallTransaction(request);
	}

	@Override
	public CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
		return parent.postStaticMethodCallTransaction(request);
	}

	@Override
	public void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException {
		parent.addInitializationTransaction(request);
	}

	@Override
	public String getConsensusConfig() throws NodeException, TimeoutException, InterruptedException {
		return parent.getConsensusConfig();
	}

	@Override
	public TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		return parent.getRequest(reference);
	}

	@Override
	public TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		return parent.getResponse(reference);
	}

	@Override
	public TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException, NodeException {
		return parent.getPolledResponse(reference);
	}

	@Override
	public Subscription subscribeToEvents(StorageReference key, BiConsumer<StorageReference, StorageReference> handler) throws UnsupportedOperationException {
		return parent.subscribeToEvents(key, handler);
	}

	@Override
	public void addOnCloseHandler(OnCloseHandler handler) {
		parent.addOnCloseHandler(handler);
	}

	@Override
	public void removeOnCloseHandler(OnCloseHandler handler) {
		parent.removeOnCloseHandler(handler);
	}
}