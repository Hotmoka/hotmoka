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
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.nodes.NodeInfo;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.helpers.api.AccountsNode;
import io.hotmoka.node.api.CodeSupplier;
import io.hotmoka.node.api.JarSupplier;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.Subscription;

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
	 */
	public AccountsNodeImpl(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, String containerClassName, TransactionReference classpath, boolean greenRed, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException {
		this.parent = parent;
		this.accounts = new StorageReference[greenRed ? funds.length / 2 : funds.length];
		this.privateKeys = new PrivateKey[accounts.length];

		StorageReference manifest = getManifest();
		var signature = SignatureHelpers.of(this).signatureAlgorithmFor(payer);
		Signer<SignedTransactionRequest> signerOnBehalfOfPayer = signature.getSigner(privateKeyOfPayer, SignedTransactionRequest::toByteArrayWithoutSignature);
		var _100_000 = BigInteger.valueOf(100_000L);
		var _200_000 = BigInteger.valueOf(200_000L);

		// we get the chainId of the parent
		String chainId = ((StringValue) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(payer, _100_000, classpath, MethodSignatures.GET_CHAIN_ID, manifest))).getValue();

		// we get the nonce of the payer
		BigInteger nonce = ((BigIntegerValue) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
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

		this.container = addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(signerOnBehalfOfPayer, payer, nonce, chainId, gas, gasHelper.getSafeGasPrice(), classpath,
			ConstructorSignatures.of(containerClassName, StorageTypes.BIG_INTEGER, StorageTypes.STRING, StorageTypes.STRING),
			StorageValues.bigIntegerOf(sum), StorageValues.stringOf(balances.toString()), StorageValues.stringOf(publicKeys.toString())));

		if (greenRed) {
			nonce = nonce.add(ONE);

			// we set the red balances of the accounts now
			addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(signerOnBehalfOfPayer, payer, nonce, chainId, gas, gasHelper.getSafeGasPrice(), classpath,
				new VoidMethodSignature(StorageTypes.ACCOUNTS, "addRedBalances", StorageTypes.BIG_INTEGER, StorageTypes.STRING),
				this.container, StorageValues.bigIntegerOf(sumRed), StorageValues.stringOf(redBalances.toString())));
		}

		var get = new NonVoidMethodSignature(StorageTypes.ACCOUNTS, "get", StorageTypes.EOA, StorageTypes.INT);

		for (int i = 0; i < funds.length / k; i++)
			this.accounts[i] = (StorageReference) runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(payer, _100_000, classpath, get, container, StorageValues.intOf(i)));
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