package io.hotmoka.nodes.internal;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
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
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.nodes.GasHelper;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.views.NodeWithAccounts;

/**
 * A decorator of a node, that creates some initial accounts in it.
 */
public class NodeWithAccountsImpl implements NodeWithAccounts {

	/**
	 * The node that is decorated.
	 */
	private final Node parent;

	/**
	 * The accounts created during initialization.
	 */
	private final StorageReference[] accounts;

	/**
	 * The private keys of the accounts created during initialization.
	 */
	private final PrivateKey[] privateKeys;

	/**
	 * The container of the accounts. This is an instance of {@code io.takamaka.code.lang.Accounts}.
	 */
	private final StorageReference container;

	/**
	 * Creates a decorated node by creating initial accounts.
	 * The transactions get payer by a given account.
	 * 
	 * @param parent the node that gets decorated
	 * @param payer the account that pays for the transactions that initialize the new accounts
	 * @param privateKeyOfPayer the private key of the account that pays for the transactions.
	 *                          It will be used to sign requests for initializing the accounts;
	 *                          the account must have enough coins to initialize the required accounts
	 * @param containerClassName the fully-qualified name of the class that must be used to contain the accounts;
	 *                           this must be {@code io.takamaka.code.lang.Accounts} or subclass
	 * @param classpath the classpath where {@code containerClassName} must be resolved
	 * @param redGreen true if red/green accounts must be created; if false, normal externally owned accounts are created
	 * @param funds the initial funds of the accounts that are created; if {@code redGreen} is true,
	 *              they must be understood in pairs, each pair for the red/green initial funds of each account (red before green)
	 * @throws TransactionRejectedException if some transaction that creates the accounts is rejected
	 * @throws TransactionException if some transaction that creates the accounts fails
	 * @throws CodeExecutionException if some transaction that creates the accounts throws an exception
	 * @throws SignatureException if some request could not be signed
	 * @throws InvalidKeyException if some key used for signing transactions is invalid
	 * @throws NoSuchAlgorithmException if the signing algorithm for the node is not available in the Java installation
	 */
	public NodeWithAccountsImpl(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, String containerClassName, TransactionReference classpath, boolean redGreen, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		this.parent = parent;
		this.accounts = new StorageReference[redGreen ? funds.length / 2 : funds.length];
		this.privateKeys = new PrivateKey[accounts.length];

		StorageReference manifest = getManifest();
		SignatureAlgorithm<SignedTransactionRequest> signature = getSignatureAlgorithmForRequests();
		Signer signerOnBehalfOfPayer = Signer.with(signature, privateKeyOfPayer);
		BigInteger _10_000 = BigInteger.valueOf(10_000L);

		// we get the chainId of the parent
		String chainId = ((StringValue) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(payer, _10_000, classpath, CodeSignature.GET_CHAIN_ID, manifest))).value;

		// we get the nonce of the payer
		BigInteger nonce = ((BigIntegerValue) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(payer, _10_000, classpath, CodeSignature.NONCE, payer))).value;

		// we create the accounts
		BigInteger gas = BigInteger.valueOf(100_000); // enough for creating an account
		GasHelper gasHelper = new GasHelper(this);

		if (redGreen) {
			BigInteger sum = ZERO;
			BigInteger sumRed = ZERO;
			StringBuilder publicKeys = new StringBuilder();
			StringBuilder balances = new StringBuilder();
			StringBuilder redBalances = new StringBuilder();
			// TODO: deal with large strings, in particular for long public keys
			for (int i = 0; i < funds.length / 2; sum = sum.add(funds[i * 2 + 1]), sumRed = sumRed.add(funds[i * 2]), i++) {
				KeyPair keys = signature.getKeyPair();
				privateKeys[i] = keys.getPrivate();
				String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
				publicKeys = publicKeys.append(i == 0 ? publicKey : (' ' + publicKey));
				balances = balances.append(i == 0 ? funds[i * 2 + 1].toString() : (' ' + funds[i * 2 + 1].toString()));
				redBalances = redBalances.append(i == 0 ? funds[i * 2].toString() : (' ' + funds[i * 2].toString()));
			}

			// we provide an amount of gas that grows linearly with the number of accounts that get created, and set the green balances of the accounts
			this.container = addConstructorCallTransaction(new ConstructorCallTransactionRequest
				(signerOnBehalfOfPayer, payer, nonce, chainId, _10_000.multiply(BigInteger.valueOf(funds.length)), gasHelper.getSafeGasPrice(), classpath,
				new ConstructorSignature(containerClassName, ClassType.BIG_INTEGER, ClassType.STRING, ClassType.STRING),
				new BigIntegerValue(sum), new StringValue(balances.toString()), new StringValue(publicKeys.toString())));

			nonce = nonce.add(ONE);

			// we set the red balances of the accounts now
			addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(signerOnBehalfOfPayer, payer, nonce, chainId, _10_000.multiply(BigInteger.valueOf(funds.length)), gasHelper.getSafeGasPrice(), classpath,
				new VoidMethodSignature(ClassType.ACCOUNTS, "addRedBalances", ClassType.BIG_INTEGER, ClassType.STRING),
				this.container, new BigIntegerValue(sumRed), new StringValue(redBalances.toString())));

			NonVoidMethodSignature get = new NonVoidMethodSignature(ClassType.ACCOUNTS, "get", ClassType.ACCOUNT, BasicTypes.INT);

			for (int i = 0; i < funds.length / 2; i++)
				this.accounts[i] = (StorageReference) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(payer, gas, classpath, get, container, new IntValue(i)));
		}
		else {
			BigInteger sum = ZERO;
			StringBuilder publicKeys = new StringBuilder();
			StringBuilder balances = new StringBuilder();
			// TODO: deal with large strings, in particular for long public keys
			for (int i = 0; i < funds.length; sum = sum.add(funds[i]), i++) {
				KeyPair keys = signature.getKeyPair();
				privateKeys[i] = keys.getPrivate();
				String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
				publicKeys = publicKeys.append(i == 0 ? publicKey : (' ' + publicKey));
				balances = balances.append(i == 0 ? funds[i].toString() : (' ' + funds[i].toString()));
			}

			// we provide an amount of gas that grows linearly with the number of accounts that get created
			this.container = addConstructorCallTransaction(new ConstructorCallTransactionRequest
				(signerOnBehalfOfPayer, payer, nonce, chainId, _10_000.multiply(BigInteger.valueOf(funds.length)), gasHelper.getSafeGasPrice(), classpath,
				new ConstructorSignature(containerClassName, ClassType.BIG_INTEGER, ClassType.STRING, ClassType.STRING),
				new BigIntegerValue(sum), new StringValue(balances.toString()), new StringValue(publicKeys.toString())));

			NonVoidMethodSignature get = new NonVoidMethodSignature(ClassType.ACCOUNTS, "get", ClassType.ACCOUNT, BasicTypes.INT);

			for (int i = 0; i < funds.length; i++)
				this.accounts[i] = (StorageReference) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(payer, gas, classpath, get, container, new IntValue(i)));
		}
	}

	@Override
	public Stream<StorageReference> accounts() {
		return Stream.of(accounts);
	}

	@Override
	public StorageReference container() {
		return container;
	}

	@Override
	public StorageReference account(int i) {
		if (i < 0 || i >= accounts.length)
			throw new NoSuchElementException();

		return accounts[i];
	}


	@Override
	public Stream<PrivateKey> privateKeys() {
		return Stream.of(privateKeys);
	}

	@Override
	public PrivateKey privateKey(int i) {
		if (i < 0 || i >= privateKeys.length)
			throw new NoSuchElementException();

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