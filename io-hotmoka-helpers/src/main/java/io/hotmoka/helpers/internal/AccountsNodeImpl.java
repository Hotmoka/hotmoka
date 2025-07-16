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

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.AbstractNodeDecorator;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.helpers.UnexpectedValueException;
import io.hotmoka.helpers.UnexpectedVoidMethodException;
import io.hotmoka.helpers.api.AccountsNode;
import io.hotmoka.helpers.api.MisbehavingNodeException;
import io.hotmoka.helpers.api.UnexpectedCodeException;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UninitializedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;

/**
 * A decorator of a node, that creates some initial accounts in it.
 */
public class AccountsNodeImpl extends AbstractNodeDecorator<Node> implements AccountsNode {

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
	 * Creates a decorated node by creating initial accounts. The transactions get paid by a given account.
	 * It allows one to specify the class of the accounts container.
	 * 
	 * @param parent the node that gets decorated
	 * @param payer the account that pays for the transactions that initialize the new accounts
	 * @param privateKeyOfPayer the private key of the account that pays for the transactions.
	 *                          It will be used to sign requests for initializing the accounts;
	 *                          the account must have enough coins to initialize the required accounts
	 * @param containerClassName the fully-qualified name of the class that must be used to contain the accounts;
	 *                           this must be {@code io.takamaka.code.lang.Accounts} or subclass
	 * @param classpath the classpath where {@code containerClassName} must be resolved
	 * @param funds the initial funds of the accounts that are created
	 * @throws TransactionRejectedException if some transaction is rejected
	 * @throws TransactionException if some transaction fails
	 * @throws CodeExecutionException if some transaction throws an exception
	 * @throws SignatureException if a signature with {@code privateKeyOfPayer} fails
	 * @throws InvalidKeyException if {@code privateKeyOfPayer} is invalid
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws UnknownReferenceException if the payer is unknown
	 * @throws NoSuchAlgorithmException if the signature algorithm of {@code payer} is not available
	 * @throws UninitializedNodeException if the node is not initialized yet
	 * @throws UnsupportedVerificationVersionException if {@code parent} uses a verification version that is not available
	 * @throws ClosedNodeException if the node is already closed
	 * @throws MisbehavingNodeException if the node is performing in a buggy way
	 * @throws UnexpectedCodeException if the node contains unexpected runtime classes installed in store
	 */
	public AccountsNodeImpl(Node parent, StorageReference payer, PrivateKey privateKeyOfPayer, String containerClassName, TransactionReference classpath, BigInteger... funds)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, UnknownReferenceException, TimeoutException, InterruptedException, NoSuchAlgorithmException, UninitializedNodeException, UnsupportedVerificationVersionException, MisbehavingNodeException, ClosedNodeException, UnexpectedCodeException {

		super(parent);

		this.accounts = new StorageReference[funds.length];
		this.privateKeys = new PrivateKey[accounts.length];

		var signature = SignatureHelpers.of(this).signatureAlgorithmFor(payer);
		Signer<SignedTransactionRequest<?>> signerOnBehalfOfPayer = signature.getSigner(privateKeyOfPayer, SignedTransactionRequest::toByteArrayWithoutSignature);
		var _100_000 = BigInteger.valueOf(100_000L);
		var _200_000 = BigInteger.valueOf(200_000L);

		// we get the chainId of the parent
		String chainId = parent.getConfig().getChainId();

		// we get the nonce of the payer
		BigInteger nonce = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(payer, _100_000, classpath, MethodSignatures.NONCE, payer))
			.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.NONCE))
			.asReturnedBigInteger(MethodSignatures.NONCE, UnexpectedValueException::new);

		var gasHelper = GasHelpers.of(this);
		BigInteger sum = ZERO;
		var publicKeys = new StringBuilder();
		var balances = new StringBuilder();

		// TODO: deal with large strings, in particular for long public keys
		for (int i = 0; i < funds.length; i++) {
			KeyPair keys = signature.getKeyPair();
			privateKeys[i] = keys.getPrivate();
			String publicKey;

			try {
				publicKey = Base64.toBase64String(signature.encodingOf(keys.getPublic()));
			}
			catch (InvalidKeyException e) {
				// we have created the key from the corresponding signature, this cannot happen
				throw new RuntimeException("Unexpected invalid key: it has been created with its same signature algorithm", e);
			}

			publicKeys.append(i == 0 ? publicKey : (' ' + publicKey));
			BigInteger fund = funds[i];
			sum = sum.add(fund);
			balances.append(i == 0 ? fund.toString() : (' ' + fund.toString()));
		}

		// we provide an amount of gas that grows linearly with the number of accounts that get created
		BigInteger gas = _200_000.multiply(BigInteger.valueOf(funds.length));

		this.container = addConstructorCallTransaction(TransactionRequests.constructorCall
			(signerOnBehalfOfPayer, payer, nonce, chainId, gas, gasHelper.getSafeGasPrice(), classpath,
			ConstructorSignatures.of(StorageTypes.classNamed(containerClassName), StorageTypes.BIG_INTEGER, StorageTypes.STRING, StorageTypes.STRING),
			StorageValues.bigIntegerOf(sum), StorageValues.stringOf(balances.toString()), StorageValues.stringOf(publicKeys.toString())));

		var get = MethodSignatures.ofNonVoid(StorageTypes.ACCOUNTS, "get", StorageTypes.EOA, StorageTypes.INT);

		for (int i = 0; i < funds.length; i++)
			this.accounts[i] = runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall(payer, _100_000, classpath, get, container, StorageValues.intOf(i)))
				.orElseThrow(() -> new UnexpectedVoidMethodException(get))
				.asReturnedReference(get, UnexpectedValueException::new);
	}

	@Override
	public Stream<StorageReference> accounts() throws ClosedNodeException {
		ensureNotClosed();
		return Stream.of(accounts);
	}

	@Override
	public StorageReference container() throws ClosedNodeException {
		ensureNotClosed();
		return container;
	}

	@Override
	public StorageReference account(int i) throws NoSuchElementException, ClosedNodeException {
		ensureNotClosed();

		if (i < 0 || i >= accounts.length)
			throw new NoSuchElementException();

		return accounts[i];
	}


	@Override
	public Stream<PrivateKey> privateKeys() throws ClosedNodeException {
		ensureNotClosed();
		return Stream.of(privateKeys);
	}

	@Override
	public PrivateKey privateKey(int i) throws NoSuchElementException, ClosedNodeException {
		ensureNotClosed();

		if (i < 0 || i >= privateKeys.length)
			throw new NoSuchElementException();

		return privateKeys[i];
	}
}