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
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.nodes.NodeInfo;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.helpers.InitializedNodes.ProducerOfStorageObject;
import io.hotmoka.helpers.api.InitializedNode;
import io.hotmoka.node.api.CodeSupplier;
import io.hotmoka.node.api.ConsensusConfig;
import io.hotmoka.node.api.JarSupplier;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.Subscription;

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

	private StorageReference createEmptyValidatorsBuilder(InitializedNode node, ConsensusConfig consensus, TransactionReference takamakaCodeReference) throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchAlgorithmException {
		var _200_000 = BigInteger.valueOf(200_000);
		var getNonceRequest = new InstanceMethodCallTransactionRequest
			(gamete, _200_000, takamakaCodeReference, CodeSignature.NONCE, gamete);
		var nonceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(getNonceRequest)).value;

		// we create the builder of zero validators
		var request = new ConstructorCallTransactionRequest
			(new byte[0], gamete, nonceOfGamete, "", _200_000, ZERO, takamakaCodeReference,
			new ConstructorSignature("io.takamaka.code.governance.GenericValidators$Builder", ClassType.STRING,
					ClassType.STRING, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, BasicTypes.LONG,
					BasicTypes.INT, BasicTypes.INT, BasicTypes.INT, BasicTypes.INT),
			new StringValue(""), new StringValue(""), new BigIntegerValue(consensus.getTicketForNewPoll()), new BigIntegerValue(consensus.getFinalSupply()),
			new LongValue(consensus.getInitialInflation()), new IntValue(consensus.getPercentStaked()),
			new IntValue(consensus.getBuyerSurcharge()), new IntValue(consensus.getSlashingForMisbehaving()), new IntValue(consensus.getSlashingForNotBehaving()));

		return node.addConstructorCallTransaction(request);
	}

	private StorageReference createGenericGasStationBuilder(InitializedNode node, ConsensusConfig consensus, TransactionReference takamakaCodeReference) throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchAlgorithmException {
		var _100_000 = BigInteger.valueOf(100_000);
		var getNonceRequest = new InstanceMethodCallTransactionRequest
			(gamete, _100_000, takamakaCodeReference, CodeSignature.NONCE, gamete);
		BigInteger nonceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(getNonceRequest)).value;

		// we create the builder of a generic gas station
		ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest
			(new byte[0], gamete, nonceOfGamete, "", _100_000, ZERO, takamakaCodeReference,
			new ConstructorSignature("io.takamaka.code.governance.GenericGasStation$Builder",
				ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, BasicTypes.BOOLEAN, ClassType.BIG_INTEGER, BasicTypes.LONG),
			new BigIntegerValue(consensus.getInitialGasPrice()), new BigIntegerValue(consensus.getMaxGasPerTransaction()),
			new BooleanValue(consensus.ignoresGasPrice()), new BigIntegerValue(consensus.getTargetGasAtReward()),
			new LongValue(consensus.getOblivion()));

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
	 */
	public InitializedNodeImpl(Node parent, ConsensusConfig consensus, Path takamakaCode,
			ProducerOfStorageObject producerOfValidatorsBuilder,
			ProducerOfStorageObject producerOfGasStationBuilder) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {

		this.parent = parent;

		// we install the jar containing the basic Takamaka classes
		TransactionReference takamakaCodeReference = parent.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaCode)));

		// we create a gamete with both red and green coins
		this.gamete = parent.addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaCodeReference, consensus.getInitialSupply(), consensus.getInitialRedSupply(), consensus.getPublicKeyOfGamete()));

		if (producerOfValidatorsBuilder == null)
			producerOfValidatorsBuilder = this::createEmptyValidatorsBuilder;

		if (producerOfGasStationBuilder == null)
			producerOfGasStationBuilder = this::createGenericGasStationBuilder;

		// we create the builder of the validators
		StorageReference builderOfValidators = producerOfValidatorsBuilder.apply(this, consensus, takamakaCodeReference);

		// we create the builder of the gas station
		StorageReference builderOfGasStation = producerOfGasStationBuilder.apply(this, consensus, takamakaCodeReference);

		var _1_000_000 = BigInteger.valueOf(1_000_000);
		var getNonceRequest = new InstanceMethodCallTransactionRequest
			(gamete, _1_000_000, takamakaCodeReference, CodeSignature.NONCE, gamete);
		BigInteger nonceOfGamete = ((BigIntegerValue) parent.runInstanceMethodCallTransaction(getNonceRequest)).value;
		var function = new ClassType(Function.class.getName());

		// we create the manifest, passing the storage array of validators in store and their powers
		var request = new ConstructorCallTransactionRequest
			(new byte[0], gamete, nonceOfGamete, "", _1_000_000, ZERO, takamakaCodeReference,
			new ConstructorSignature(ClassType.MANIFEST, ClassType.STRING, ClassType.STRING, BasicTypes.INT,
				BasicTypes.INT, BasicTypes.LONG,
				BasicTypes.BOOLEAN, BasicTypes.BOOLEAN, BasicTypes.BOOLEAN, BasicTypes.BOOLEAN,
				ClassType.STRING, ClassType.GAMETE, BasicTypes.INT, function, function),
			new StringValue(consensus.getGenesisTime()),
			new StringValue(consensus.getChainId()), new IntValue(consensus.getMaxErrorLength()), new IntValue(consensus.getMaxDependencies()),
			new LongValue(consensus.getMaxCumulativeSizeOfDependencies()), new BooleanValue(consensus.allowsSelfCharged()),
			new BooleanValue(consensus.allowsUnsignedFaucet()), new BooleanValue(consensus.allowsMintBurnFromGamete()),
			new BooleanValue(consensus.skipsVerification()),
			new StringValue(consensus.getSignature()), gamete, new IntValue(consensus.getVerificationVersion()),
			builderOfValidators, builderOfGasStation);

		StorageReference manifest = parent.addConstructorCallTransaction(request);

		// we install the manifest and initialize the node
		parent.addInitializationTransaction(new InitializationTransactionRequest(takamakaCodeReference, manifest));
	}

	@Override
	public StorageReference gamete() {
		return gamete;
	}

	@Override
	public void close() throws Exception {
		parent.close();
	}

	@Override
	public StorageReference getManifest() throws NoSuchElementException {
		return parent.getManifest();
	}

	@Override
	public TransactionReference getTakamakaCode() {
		return parent.getTakamakaCode();
	}

	@Override
	public NodeInfo getNodeInfo() {
		return parent.getNodeInfo();
	}

	@Override
	public ClassTag getClassTag(StorageReference reference) throws NoSuchElementException {
		return parent.getClassTag(reference);
	}

	@Override
	public Stream<Update> getState(StorageReference reference) throws NoSuchElementException {
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
	public StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.runInstanceMethodCallTransaction(request);
	}

	@Override
	public StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
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
	public String getNameOfSignatureAlgorithmForRequests() {
		return parent.getNameOfSignatureAlgorithmForRequests();
	}

	@Override
	public TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException {
		return parent.getRequest(reference);
	}

	@Override
	public TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
		return parent.getResponse(reference);
	}

	@Override
	public TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {
		return parent.getPolledResponse(reference);
	}

	@Override
	public Subscription subscribeToEvents(StorageReference key, BiConsumer<StorageReference, StorageReference> handler) throws UnsupportedOperationException {
		return parent.subscribeToEvents(key, handler);
	}
}