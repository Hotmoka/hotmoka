package io.hotmoka.nodes.internal;

import static java.math.BigInteger.ONE;
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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
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
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest.Signer;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.Validator;
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

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * A brand new key pair is generated, for controlling the gamete. No validators are stored in the manifest.
	 * 
	 * @param parent the node to decorate
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @param manifestClassName the name of the class of the manifest set for the node
	 * @param chainId the initial chainId set for the node, inside its manifest
	 * @param greenAmount the amount of green coins that must be put in the gamete
	 * @param redAmount the amount of red coins that must be put in the gamete
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some initialization request could not be signed
	 * @throws InvalidKeyException if some key used for signing initialization transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	public InitializedNodeImpl(Node parent, Path takamakaCode, String manifestClassName, String chainId, BigInteger greenAmount, BigInteger redAmount) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		this(parent, parent.getSignatureAlgorithmForRequests().getKeyPair(), Stream.empty(), takamakaCode, manifestClassName, chainId, greenAmount, redAmount);
	}

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * A brand new key pair is generated, for controlling the gamete. No validators are stored in the manifest.
	 * 
	 * @param parent the node to decorate
	 * @param keysOfGamete the key pair that will be used to control the gamete
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @param manifestClassName the name of the class of the manifest set for the node
	 * @param chainId the initial chainId set for the node, inside its manifest
	 * @param greenAmount the amount of green coins that must be put in the gamete
	 * @param redAmount the amount of red coins that must be put in the gamete
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some initialization request could not be signed
	 * @throws InvalidKeyException if some key used for signing initialization transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	public InitializedNodeImpl(Node parent, KeyPair keysOfGamete, Path takamakaCode, String manifestClassName, String chainId, BigInteger greenAmount, BigInteger redAmount) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		this(parent, keysOfGamete, Stream.empty(), takamakaCode, manifestClassName, chainId, greenAmount, redAmount);
	}

	/**
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
	 * Uses the given key pair for controlling the gamete.
	 * 
	 * @param parent the node to decorate
	 * @param keysOfGamete the key pair that will be used to control the gamete
	 * @param validators the list of validators that will be stored in the manifest
	 * @param takamakaCode the jar containing the basic Takamaka classes
	 * @param manifestClassName the name of the class of the manifest set for the node
	 * @param chainId the initial chainId set for the node, inside its manifest
	 * @param greenAmount the amount of green coins that must be put in the gamete
	 * @param redAmount the amount of red coins that must be put in the gamete
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some initialization request could not be signed
	 * @throws InvalidKeyException if some key used for signing initialization transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	public InitializedNodeImpl(Node parent, KeyPair keysOfGamete, Stream<Validator> validators, Path takamakaCode, String manifestClassName, String chainId, BigInteger greenAmount, BigInteger redAmount) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		this.parent = parent;

		// we install the jar containing the basic Takamaka classes
		TransactionReference takamakaCodeReference = parent.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaCode)));
		this.keysOfGamete = keysOfGamete;

		// we create a gamete with both red and green coins
		String publicKeyOfGameteBase64Encoded = Base64.getEncoder().encodeToString(keysOfGamete.getPublic().getEncoded());
		this.gamete = parent.addRedGreenGameteCreationTransaction(new RedGreenGameteCreationTransactionRequest(takamakaCodeReference, greenAmount, redAmount, publicKeyOfGameteBase64Encoded));

		List<Validator> validatorsAsList = validators.collect(Collectors.toList());
		SignatureAlgorithm<NonInitialTransactionRequest<?>> signature = parent.getSignatureAlgorithmForRequests();
		Signer signer = Signer.with(signature, keysOfGamete);
		BigInteger nonceOfGamete = ZERO;
		BigInteger _10_000 = BigInteger.valueOf(10_000);

		// we create the storage array; we use "" as chainId, since it is not assigned yet
		ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest
				(signer, gamete, nonceOfGamete, "", _10_000, ZERO, takamakaCodeReference,
				new ConstructorSignature(ClassType.STORAGE_ARRAY, BasicTypes.INT),
				new IntValue(validatorsAsList.size()));

		StorageReference array = parent.addConstructorCallTransaction(request);
		nonceOfGamete = nonceOfGamete.add(ONE);

		// we create a validator object in the store of the node, for each element in validators;
		// these are the accounts that can receive payments if they correctly validate transactions,
		// depending on the kind of node (each node has its own policy);
		// all such objects are put inside the StorageArray, then passed to the manifest
		int pos = 0;
		for (Validator validator: validatorsAsList) {
			StorageReference validatorInStore = createValidatorInStore(validator, signer, nonceOfGamete, takamakaCodeReference);
			nonceOfGamete = nonceOfGamete.add(ONE);

			// we set the pos-th element of the storage array
			InstanceMethodCallTransactionRequest setRequest = new InstanceMethodCallTransactionRequest
				(signer, gamete, nonceOfGamete, "", _10_000, ZERO, takamakaCodeReference,
				new VoidMethodSignature(ClassType.STORAGE_ARRAY, "set", BasicTypes.INT, ClassType.OBJECT),
				array, new IntValue(pos++), validatorInStore);

			parent.addInstanceMethodCallTransaction(setRequest);
			nonceOfGamete = nonceOfGamete.add(ONE);
		}

		// we finally create the manifest, passing the storage array of validators in store
		request = new ConstructorCallTransactionRequest
			(signer, gamete, nonceOfGamete, "", _10_000, ZERO, takamakaCodeReference,
			new ConstructorSignature(manifestClassName, ClassType.STRING),
			new StringValue(chainId));

		StorageReference manifest = parent.addConstructorCallTransaction(request);

		// we install the manifest and initialize the node
		parent.addInitializationTransaction(new InitializationTransactionRequest(takamakaCodeReference, manifest));
	}

	/**
	 * Creates a validator object in the store of the node, corresponding to the description
	 * in the parameter. The gamete pays for that.
	 * 
	 * @param validator the description of the validator to create
	 * @param signer the signer on behalf of the gamete
	 * @param nonceOfGamete the nonce to use for the gamete
	 * @param takamakaCodeReference the reference to the transaction that installed the base Takamaka classes in the store of the node
	 * @return the reference to the object created in store
	 * @throws SignatureException 
	 * @throws InvalidKeyException 
	 * @throws CodeExecutionException 
	 * @throws TransactionException 
	 * @throws TransactionRejectedException 
	 */
	private StorageReference createValidatorInStore(Validator validator, Signer signer, BigInteger nonceOfGamete, TransactionReference takamakaCodeReference) throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException {
		String publicKeyOfValidatorBase64Encoded = Base64.getEncoder().encodeToString(validator.publicKey.getEncoded());

		ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest
			(signer, gamete, nonceOfGamete, "", BigInteger.valueOf(10_000), ZERO, takamakaCodeReference,
			new ConstructorSignature(ClassType.VALIDATOR, ClassType.STRING, BasicTypes.LONG, ClassType.STRING),
			new StringValue(validator.id), new LongValue(validator.power), new StringValue(publicKeyOfValidatorBase64Encoded));

		return parent.addConstructorCallTransaction(request);
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
	public SignatureAlgorithm<NonInitialTransactionRequest<?>> getSignatureAlgorithmForRequests() throws NoSuchAlgorithmException {
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