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

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
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
import io.hotmoka.beans.api.requests.SignedTransactionRequest;
import io.hotmoka.beans.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.updates.ClassTag;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.closeables.api.OnCloseHandler;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.helpers.api.AccountsNode;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.CodeSupplier;
import io.hotmoka.node.api.JarSupplier;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * A decorator of a node, that creates some initial accounts in it.
 */
public class AccountsNodeImpl implements AccountsNode {

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
	 * True if and only if this node has been closed already.
	 */
	private final AtomicBoolean isClosed = new AtomicBoolean();

	/**
	 * We need this intermediate definition since two instances of a method reference
	 * are not the same, nor equals.
	 */
	private final OnCloseHandler this_close = this::close;

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
	 * @param greenRed true if both green and red balances must be initialized; if false, only the green balance is initialized
	 * @param funds the initial funds of the accounts that are created; if {@code greenRed} is true,
	 *              they must be understood in pairs, each pair for the green and red initial funds of each account (green before red)
	 * @throws TransactionRejectedException if some transaction that creates the accounts is rejected
	 * @throws TransactionException if some transaction that creates the accounts fails
	 * @throws CodeExecutionException if some transaction that creates the accounts throws an exception
	 * @throws SignatureException if some request could not be signed
	 * @throws InvalidKeyException if some key used for signing transactions is invalid
	 * @throws NoSuchAlgorithmException if the payer uses an unknown signature algorithm
	 * @throws ClassNotFoundException if the class of the payer cannot be determined
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws NoSuchElementException if the node is not properly initialized
	 */
	public AccountsNodeImpl(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, String containerClassName, TransactionReference classpath, boolean greenRed, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException, NodeException, NoSuchElementException, TimeoutException, InterruptedException {
		this.parent = parent;
		this.accounts = new StorageReference[greenRed ? funds.length / 2 : funds.length];
		this.privateKeys = new PrivateKey[accounts.length];

		StorageReference manifest = getManifest();
		var signature = SignatureHelpers.of(this).signatureAlgorithmFor(payer);
		Signer<SignedTransactionRequest<?>> signerOnBehalfOfPayer = signature.getSigner(privateKeyOfPayer, SignedTransactionRequest::toByteArrayWithoutSignature);
		var _100_000 = BigInteger.valueOf(100_000L);
		var _200_000 = BigInteger.valueOf(200_000L);

		// we get the chainId of the parent
		String chainId = ((StringValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(payer, _100_000, classpath, MethodSignatures.GET_CHAIN_ID, manifest))).getValue();

		// we get the nonce of the payer
		BigInteger nonce = ((BigIntegerValue) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(payer, _100_000, classpath, MethodSignatures.NONCE, payer))).getValue();

		var gasHelper = GasHelpers.of(this);
		BigInteger sum = ZERO;
		BigInteger sumRed = ZERO;
		var publicKeys = new StringBuilder();
		var balances = new StringBuilder();
		var redBalances = new StringBuilder();
		int k = greenRed ? 2 : 1;

		// TODO: deal with large strings, in particular for long public keys
		for (int i = 0; i < funds.length / k; i++) {
			KeyPair keys = signature.getKeyPair();
			privateKeys[i] = keys.getPrivate();
			String publicKey = Base64.toBase64String(signature.encodingOf(keys.getPublic()));
			publicKeys.append(i == 0 ? publicKey : (' ' + publicKey));
			BigInteger fund = funds[i * k];
			sum = sum.add(fund);
			balances.append(i == 0 ? fund.toString() : (' ' + fund.toString()));

			if (greenRed) {
				fund = funds[i * 2 + 1];
				sumRed = sumRed.add(fund);
				redBalances.append(i == 0 ? fund.toString() : (' ' + fund.toString()));
			}
		}

		// we provide an amount of gas that grows linearly with the number of accounts that get created, and set the green balances of the accounts
		BigInteger gas = _200_000.multiply(BigInteger.valueOf(funds.length / k));

		this.container = addConstructorCallTransaction(TransactionRequests.constructorCall
			(signerOnBehalfOfPayer, payer, nonce, chainId, gas, gasHelper.getSafeGasPrice(), classpath,
			ConstructorSignatures.of(containerClassName, StorageTypes.BIG_INTEGER, StorageTypes.STRING, StorageTypes.STRING),
			StorageValues.bigIntegerOf(sum), StorageValues.stringOf(balances.toString()), StorageValues.stringOf(publicKeys.toString())));

		if (greenRed) {
			nonce = nonce.add(ONE);

			// we set the red balances of the accounts now
			addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall
				(signerOnBehalfOfPayer, payer, nonce, chainId, gas, gasHelper.getSafeGasPrice(), classpath,
				MethodSignatures.ofVoid(StorageTypes.ACCOUNTS, "addRedBalances", StorageTypes.BIG_INTEGER, StorageTypes.STRING),
				this.container, StorageValues.bigIntegerOf(sumRed), StorageValues.stringOf(redBalances.toString())));
		}

		var get = MethodSignatures.of(StorageTypes.ACCOUNTS, "get", StorageTypes.EOA, StorageTypes.INT);

		for (int i = 0; i < funds.length / k; i++)
			this.accounts[i] = (StorageReference) runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(payer, _100_000, classpath, get, container, StorageValues.intOf(i)));

		// when the parent is closed, this decorator will be closed as well
		parent.addOnCloseHandler(this_close);
	}

	@Override
	public Stream<StorageReference> accounts() { // TODO: throw exception if closed
		return Stream.of(accounts);
	}

	@Override
	public StorageReference container() { // TODO: throw exception if closed
		return container;
	}

	@Override
	public StorageReference account(int i) { // TODO: throw exception if closed
		if (i < 0 || i >= accounts.length)
			throw new NoSuchElementException();

		return accounts[i];
	}


	@Override
	public Stream<PrivateKey> privateKeys() { // TODO: throw exception if closed
		return Stream.of(privateKeys);
	}

	@Override
	public PrivateKey privateKey(int i) { // TODO: throw exception if closed
		if (i < 0 || i >= privateKeys.length)
			throw new NoSuchElementException();

		return privateKeys[i];
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