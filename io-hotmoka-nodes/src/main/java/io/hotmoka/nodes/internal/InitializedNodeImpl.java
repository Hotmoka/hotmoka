package io.hotmoka.nodes.internal;

import static java.math.BigInteger.ZERO;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
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
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.views.InitializedNode;

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
	 * The keys generated for signing requests on behalf of the gamete.
	 */
	private final KeyPair keysOfGamete;

	/**
	 * The gamete that has been generated.
	 */
	private final StorageReference gamete;

	private static StorageReference createEmptyValidatorsBuilder(InitializedNode node, ConsensusParams consensus, TransactionReference takamakaCodeReference) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException {
		SignatureAlgorithm<SignedTransactionRequest> signature = node.getSignatureAlgorithmForRequests();
		Signer signer = Signer.with(signature, node.keysOfGamete());
		StorageReference gamete = node.gamete();

		BigInteger _100_000 = BigInteger.valueOf(100_000);
		InstanceMethodCallTransactionRequest getNonceRequest = new InstanceMethodCallTransactionRequest
			(gamete, _100_000, takamakaCodeReference, CodeSignature.NONCE, gamete);
		BigInteger nonceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(getNonceRequest)).value;

		// we create the builder of zero validators
		ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest
			(signer, gamete, nonceOfGamete, "", _100_000, ZERO, takamakaCodeReference,
			new ConstructorSignature("io.takamaka.code.system.GenericValidators$Builder", ClassType.STRING, ClassType.STRING),
			new StringValue(""), new StringValue(""));

		return node.addConstructorCallTransaction(request);
	}

	private static StorageReference createGenericGasStationBuilder(InitializedNode node, ConsensusParams consensus, TransactionReference takamakaCodeReference) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, TransactionRejectedException, TransactionException, CodeExecutionException {
		SignatureAlgorithm<SignedTransactionRequest> signature = node.getSignatureAlgorithmForRequests();
		Signer signer = Signer.with(signature, node.keysOfGamete());
		StorageReference gamete = node.gamete();

		BigInteger _100_000 = BigInteger.valueOf(100_000);
		InstanceMethodCallTransactionRequest getNonceRequest = new InstanceMethodCallTransactionRequest
			(gamete, _100_000, takamakaCodeReference, CodeSignature.NONCE, gamete);
		BigInteger nonceOfGamete = ((BigIntegerValue) node.runInstanceMethodCallTransaction(getNonceRequest)).value;

		// we create the builder of a generic gas station
		ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest
			(signer, gamete, nonceOfGamete, "", _100_000, ZERO, takamakaCodeReference,
			new ConstructorSignature("io.takamaka.code.system.GenericGasStation$Builder", ClassType.BIG_INTEGER, BasicTypes.BOOLEAN, ClassType.BIG_INTEGER, BasicTypes.LONG),
			new BigIntegerValue(consensus.maxGasPerTransaction), new BooleanValue(consensus.ignoresGasPrice),
			new BigIntegerValue(consensus.targetGasAtReward), new LongValue(consensus.oblivion));

		return node.addConstructorCallTransaction(request);
	}

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given key pair for controlling the gamete.
	 * 
	 * @param parent the node to decorate
	 * @param consensus the consensus parameters that will be set for the node
	 * @param keysOfGamete the key pair that will be used to control the gamete
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @param greenAmount the amount of green coins that must be put in the gamete
	 * @param redAmount the amount of red coins that must be put in the gamete
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
	public InitializedNodeImpl(Node parent, ConsensusParams consensus, KeyPair keysOfGamete, Path takamakaCode,
			BigInteger greenAmount, BigInteger redAmount,
			ProducerOfStorageObject producerOfValidatorsBuilder,
			ProducerOfStorageObject producerOfGasStationBuilder) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {

		this.parent = parent;

		// we install the jar containing the basic Takamaka classes
		TransactionReference takamakaCodeReference = parent.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaCode)));
		this.keysOfGamete = keysOfGamete;

		// we create a gamete with both red and green coins
		String publicKeyOfGameteBase64Encoded = Base64.getEncoder().encodeToString(keysOfGamete.getPublic().getEncoded());
		this.gamete = parent.addRedGreenGameteCreationTransaction(new RedGreenGameteCreationTransactionRequest(takamakaCodeReference, greenAmount, redAmount, publicKeyOfGameteBase64Encoded));

		if (producerOfValidatorsBuilder == null)
			producerOfValidatorsBuilder = InitializedNodeImpl::createEmptyValidatorsBuilder;

		if (producerOfGasStationBuilder == null)
			producerOfGasStationBuilder = InitializedNodeImpl::createGenericGasStationBuilder;

		// we create the builder of the validators
		StorageReference builderOfValidators = producerOfValidatorsBuilder.apply(this, consensus, takamakaCodeReference);

		// we create the builder of the gas station
		StorageReference builderOfGasStation = producerOfGasStationBuilder.apply(this, consensus, takamakaCodeReference);

		SignatureAlgorithm<SignedTransactionRequest> signature = parent.getSignatureAlgorithmForRequests();
		Signer signer = Signer.with(signature, keysOfGamete);
		BigInteger _100_000 = BigInteger.valueOf(100_000);
		InstanceMethodCallTransactionRequest getNonceRequest = new InstanceMethodCallTransactionRequest
			(gamete, _100_000, takamakaCodeReference, CodeSignature.NONCE, gamete);
		BigInteger nonceOfGamete = ((BigIntegerValue) parent.runInstanceMethodCallTransaction(getNonceRequest)).value;
		ClassType function = new ClassType(Function.class.getName());

		// we create the manifest, passing the storage array of validators in store and their powers
		ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest
			(signer, gamete, nonceOfGamete, "", _100_000, ZERO, takamakaCodeReference,
			new ConstructorSignature(ClassType.MANIFEST, ClassType.STRING, BasicTypes.INT,
				BasicTypes.INT, BasicTypes.LONG,
				BasicTypes.BOOLEAN, ClassType.STRING, ClassType.ACCOUNT,
				BasicTypes.INT, function, function),
			new StringValue(consensus.chainId), new IntValue(consensus.maxErrorLength), new IntValue(consensus.maxDependencies),
			new LongValue(consensus.maxCumulativeSizeOfDependencies), new BooleanValue(consensus.allowsSelfCharged),
			new StringValue(consensus.getSignature().getName()), gamete, new IntValue(consensus.verificationVersion), builderOfValidators, builderOfGasStation);

		StorageReference manifest = parent.addConstructorCallTransaction(request);

		// we install the manifest and initialize the node
		parent.addInitializationTransaction(new InitializationTransactionRequest(takamakaCodeReference, manifest));
	}

	@Override
	public KeyPair keysOfGamete() {
		return keysOfGamete;
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
	public StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
		return parent.addRedGreenGameteCreationTransaction(request);
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
	public SignatureAlgorithm<SignedTransactionRequest> getSignatureAlgorithmForRequests() {
		return parent.getSignatureAlgorithmForRequests();
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