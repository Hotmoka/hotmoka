package io.hotmoka.nodes.internal;

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
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.SignatureAlgorithm;
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
	 * Creates a decorated node with basic Takamaka classes, gamete and manifest.
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
		this.parent = parent;

		// we install the jar containing the basic Takamaka classes
		TransactionReference takamakaCodeReference = parent.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(takamakaCode)));

		SignatureAlgorithm<NonInitialTransactionRequest<?>> signature = parent.getSignatureAlgorithmForRequests();
		this.keysOfGamete = signature.getKeyPair();

		// we create a gamete with both red and green coins
		String publicKeyOfGameteBase64Encoded = Base64.getEncoder().encodeToString(keysOfGamete.getPublic().getEncoded());
		StorageReference gamete = parent.addRedGreenGameteCreationTransaction(new RedGreenGameteCreationTransactionRequest(takamakaCodeReference, greenAmount, redAmount, publicKeyOfGameteBase64Encoded));

		// we create the manifest
		KeyPair keysOfManifest = signature.getKeyPair();
		String publicKeyOfManifestBase64Encoded = Base64.getEncoder().encodeToString(keysOfManifest.getPublic().getEncoded());
		// we use "" as chainId, since it is not assigned yet
		ConstructorCallTransactionRequest request = new ConstructorCallTransactionRequest
			(Signer.with(signature, keysOfGamete), gamete, BigInteger.ZERO, "", BigInteger.valueOf(10_000), BigInteger.ZERO, takamakaCodeReference,
			new ConstructorSignature(manifestClassName, ClassType.RGEOA, ClassType.STRING, ClassType.STRING),
			gamete, new StringValue(publicKeyOfManifestBase64Encoded), new StringValue(chainId));

		StorageReference manifest = parent.addConstructorCallTransaction(request);

		// we install the manifest and initialize the node
		parent.addInitializationTransaction(new InitializationTransactionRequest(takamakaCodeReference, manifest));
	}

	@Override
	public KeyPair keysOfGamete() {
		return keysOfGamete;
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
	public TransactionRequest<?> getRequestAt(TransactionReference reference) throws NoSuchElementException {
		return parent.getRequestAt(reference);
	}

	@Override
	public TransactionResponse getResponseAt(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException {
		return parent.getResponseAt(reference);
	}

	@Override
	public TransactionResponse getPolledResponseAt(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException {
		return parent.getPolledResponseAt(reference);
	}
}