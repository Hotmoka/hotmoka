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
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.views.NodeWithAccounts;
import io.takamaka.code.constants.Constants;

/**
 * A decorator of a node, that installs a jar and creates some initial accounts in it.
 * It is mainly useful for testing.
 */
public class NodeWithAccountsImpl implements NodeWithAccounts {

	/**
	 * The node that is decorated.
	 */
	protected final Node parent;

	/**
	 * The classpath of a user jar that has been installed, if any.
	 */
	public final TransactionReference jar;

	/**
	 * The accounts created during initialization.
	 */
	private final StorageReference[] accounts;

	/**
	 * The private keys of the accounts created during initialization.
	 */
	private final PrivateKey[] privateKeys;

	/**
	 * The method of red/green contracts to send red coins.
	 */
	private final static VoidMethodSignature RECEIVE_RED = new VoidMethodSignature(ClassType.RGPAYABLE_CONTRACT, "receiveRed", ClassType.BIG_INTEGER);

	/**
	 * The constructor of an externally owned account.
	 */
	private final static ConstructorSignature TEOA_CONSTRUCTOR = new ConstructorSignature(ClassType.TEOA, ClassType.BIG_INTEGER);

	/**
	 * The constructor of an externally owned account with red/green funds.
	 */
	private final static ConstructorSignature TRGEOA_CONSTRUCTOR = new ConstructorSignature(ClassType.TRGEOA, ClassType.BIG_INTEGER);

	/**
	 * Creates a decorated node by storing into it a jar and creating initial accounts.
	 * 
	 * @param parent the node that gets decorated
	 * @param privateKeyOfGamete the private key of the gamete, that is needed to sign requests for initializing the accounts;
	 *                           the gamete must have enough coins to initialize the required accounts
	 * @param jar the path of a jar that must be further installed in blockchain. This might be {@code null}
	 * @param redGreen true if red/green accounts must be created; if false, normal externally owned accounts are created
	 * @param funds the initial funds of the accounts that are created; if {@code redGreen} is true,
	 *              they must be understood in pairs, each pair for the red/green initial funds of each account (red before green)
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 * @throws SignatureException if some request could not be signed
	 * @throws InvalidKeyException if some key used for signing transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	public NodeWithAccountsImpl(Node parent, PrivateKey privateKeyOfGamete, Path jar, boolean redGreen, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		this.parent = parent;
		this.accounts = new StorageReference[redGreen ? funds.length / 2 : funds.length];
		this.privateKeys = new PrivateKey[accounts.length];

		TransactionReference takamakaCode = getTakamakaCode();
		StorageReference manifest = getManifest();
		SignatureAlgorithm<NonInitialTransactionRequest<?>> signature = signatureAlgorithmForRequests();
		Signer signerOnBehalfOfGamete = Signer.with(signature, privateKeyOfGamete);

		// we get the nonce of the manifest account
		BigInteger nonce = ((BigIntegerValue) parent.runViewInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(Signer.onBehalfOfManifest(), manifest, ZERO, BigInteger.valueOf(10_000), ZERO, takamakaCode, new NonVoidMethodSignature(Constants.ACCOUNT_NAME, "nonce", ClassType.BIG_INTEGER), manifest))).value;

		// we call its getGamete() method
		StorageReference gamete = (StorageReference) parent.runViewInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(Signer.onBehalfOfManifest(), manifest, nonce, BigInteger.valueOf(10_000), ZERO, takamakaCode, new NonVoidMethodSignature(Constants.MANIFEST_NAME, "getGamete", ClassType.RGEOA), manifest));

		// we get the nonce of the gamete
		nonce = ((BigIntegerValue) runViewInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(signerOnBehalfOfGamete, gamete, ZERO, BigInteger.valueOf(10_000), ZERO, takamakaCode, new NonVoidMethodSignature(Constants.ACCOUNT_NAME, "nonce", ClassType.BIG_INTEGER), gamete))).value;

		JarSupplier jarSupplier;
		if (jar != null) {
			jarSupplier = postJarStoreTransaction(new JarStoreTransactionRequest(signerOnBehalfOfGamete, gamete, nonce, BigInteger.valueOf(1_000_000_000), ZERO, takamakaCode, Files.readAllBytes(jar), takamakaCode));
			nonce = nonce.add(ONE);
		}
		else
			jarSupplier = null;

		// we create the accounts
		BigInteger gas = BigInteger.valueOf(10_000); // enough for creating an account
		List<CodeSupplier<StorageReference>> accounts = new ArrayList<>();

		if (redGreen)
			for (int i = 1; i < funds.length; i += 2, nonce = nonce.add(ONE)) {
				KeyPair keys = signature.getKeyPair();
				privateKeys[(i - 1) / 2] = keys.getPrivate();
				// the constructor provides the green coins
				accounts.add(postConstructorCallTransaction(new ConstructorCallTransactionRequest
					(signerOnBehalfOfGamete, gamete, nonce, gas, ZERO, takamakaCode, TRGEOA_CONSTRUCTOR, new BigIntegerValue(funds[i]))));
			}
		else
			for (int i = 0; i < funds.length; i++, nonce = nonce.add(ONE)) {
				KeyPair keys = signature.getKeyPair();
				privateKeys[i] = keys.getPrivate();
				accounts.add(postConstructorCallTransaction(new ConstructorCallTransactionRequest
					(signerOnBehalfOfGamete, gamete, nonce, gas, ZERO, takamakaCode, TEOA_CONSTRUCTOR, new BigIntegerValue(funds[i]))));
			}

		int i = 0;
		for (CodeSupplier<StorageReference> account: accounts) {
			this.accounts[i] = account.get();

			if (redGreen) {
				// we add the red coins
				postInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(signerOnBehalfOfGamete, gamete, nonce, gas, ZERO, takamakaCode,
					RECEIVE_RED, this.accounts[i], new BigIntegerValue(funds[i * 2])));

				nonce = nonce.add(ONE);
			}

			i++;
		}

		this.jar = jarSupplier != null ? jarSupplier.get() : null;
	}

	@Override
	public Optional<TransactionReference> jar() {
		return Optional.ofNullable(jar);
	}

	@Override
	public StorageReference account(int i) {
		return accounts[i];
	}


	@Override
	public PrivateKey privateKey(int i) {
		return privateKeys[i];
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
	public long getNow() {
		return parent.getNow();
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
	public StorageValue runViewInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.runViewInstanceMethodCallTransaction(request);
	}

	@Override
	public StorageValue runViewStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.runViewStaticMethodCallTransaction(request);
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
	public SignatureAlgorithm<NonInitialTransactionRequest<?>> signatureAlgorithmForRequests() throws NoSuchAlgorithmException {
		return parent.signatureAlgorithmForRequests();
	}
}