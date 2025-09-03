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

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Logger;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.helpers.UnexpectedValueException;
import io.hotmoka.helpers.UnexpectedVoidMethodException;
import io.hotmoka.helpers.api.AccountCreationHelper;
import io.hotmoka.helpers.api.GasHelper;
import io.hotmoka.helpers.api.MisbehavingNodeException;
import io.hotmoka.helpers.api.NonceHelper;
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
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;

/**
 * An object that helps with the creation of new accounts.
 */
public class AccountCreationHelperImpl implements AccountCreationHelper {
	private final Node node;
	private final StorageReference manifest;
	private final TransactionReference takamakaCode;
	private final NonceHelper nonceHelper;
	private final GasHelper gasHelper;
	private final String chainId;
	private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);
	private final static Logger LOGGER = Logger.getLogger(AccountCreationHelperImpl.class.getName());

	/**
	 * Creates an object that helps with the creation of new accounts.
	 * 
	 * @param node the node whose accounts are considered
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws ClosedNodeException if the node is already closed
	 * @throws CodeExecutionException if some transaction threw an exception
	 * @throws TransactionException if some transaction failed
	 * @throws TransactionRejectedException if some transaction has been rejected
	 * @throws UninitializedNodeException if the node is not initialized yet
	 * @throws UnexpectedCodeException if the Takamaka runtime in the node is behaving in an unexpected way
	 */
	public AccountCreationHelperImpl(Node node) throws ClosedNodeException, TimeoutException, InterruptedException, TransactionRejectedException, TransactionException, CodeExecutionException, UninitializedNodeException, UnexpectedCodeException {
		this.node = node;
		this.manifest = node.getManifest();
		this.takamakaCode = node.getTakamakaCode();
		this.nonceHelper = NonceHelpers.of(node);
		this.gasHelper = GasHelpers.of(node);
		this.chainId = node.getConfig().getChainId();
	}

	@Override
	public StorageReference paidByFaucet(SignatureAlgorithm signature, PublicKey publicKey,
			BigInteger balance, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, InterruptedException, TimeoutException, MisbehavingNodeException, ClosedNodeException, UnexpectedCodeException {

		StorageReference gamete = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest))
			.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.GET_GAMETE))
			.asReturnedReference(MethodSignatures.GET_GAMETE, UnexpectedValueException::new);

		BigInteger gas = gasForCreatingAccountWithSignature(signature);
		String methodName = "faucet" + signature.getName().toUpperCase();
		ClassType eoaType = StorageTypes.classNamed(StorageTypes.EOA + signature.getName().toUpperCase());

		// we use an empty signature algorithm and an arbitrary key, since the faucet is unsigned
		var signatureForFaucet = SignatureAlgorithms.empty();
		KeyPair keyPair = signatureForFaucet.getKeyPair();
		Signer<SignedTransactionRequest<?>> signer = signatureForFaucet.getSigner(keyPair.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);
		String publicKeyEncoded = Base64.toBase64String(signature.encodingOf(publicKey));
		var method = MethodSignatures.ofNonVoid(StorageTypes.GAMETE, methodName, eoaType, StorageTypes.BIG_INTEGER, StorageTypes.STRING);
		BigInteger nonce;

		try {
			nonce = nonceHelper.getNonceOf(gamete);
		}
		catch (UnknownReferenceException e) {
			throw new MisbehavingNodeException("The gamete cannot be found in its same node, or is not an account", e);
		}

		InstanceMethodCallTransactionRequest request;

		try {
			request = TransactionRequests.instanceMethodCall
				(signer, gamete, nonce, chainId, gas, gasHelper.getGasPrice(), takamakaCode,
				method, gamete, StorageValues.bigIntegerOf(balance), StorageValues.stringOf(publicKeyEncoded));
		}
		catch (SignatureException e) {
			throw new RuntimeException("Transaction signing failed, unexpectedly, for the empty signature", e);
		}

		var account = node.addInstanceMethodCallTransaction(request)
			.orElseThrow(() -> new UnexpectedVoidMethodException(method))
			.asReturnedReference(method, UnexpectedValueException::new);

		requestsHandler.accept(new TransactionRequest<?>[] { request });

		return account;
	}

	@Override
	public StorageReference paidBy(StorageReference payer, KeyPair keysOfPayer, SignatureAlgorithm signature, PublicKey publicKey, BigInteger balance, Consumer<BigInteger> gasHandler, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, TimeoutException, InterruptedException, UnknownReferenceException, NoSuchAlgorithmException, UnsupportedVerificationVersionException, MisbehavingNodeException, ClosedNodeException, UnexpectedCodeException {

		ClassType eoaType = StorageTypes.classNamed(StorageTypes.EOA + signature.getName().toUpperCase());
		var signatureForPayer = SignatureHelpers.of(node).signatureAlgorithmFor(payer);

		BigInteger gas1 = gasForCreatingAccountWithSignature(signature);
		BigInteger gas2 = gasForTransactionWhosePayerHasSignature(signatureForPayer.getName());
		BigInteger totalGas = gas1.add(gas2);

		gasHandler.accept(totalGas);

		String publicKeyEncoded = Base64.toBase64String(signature.encodingOf(publicKey));
		StorageReference account;
		Signer<SignedTransactionRequest<?>> signer = signatureForPayer.getSigner(keysOfPayer.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

		ConstructorCallTransactionRequest request = TransactionRequests.constructorCall
				(signer, payer, nonceHelper.getNonceOf(payer),
						chainId, totalGas, gasHelper.getGasPrice(), takamakaCode,
						ConstructorSignatures.of(eoaType, StorageTypes.BIG_INTEGER, StorageTypes.STRING),
						StorageValues.bigIntegerOf(balance), StorageValues.stringOf(publicKeyEncoded));

		account = node.addConstructorCallTransaction(request);
		requestsHandler.accept(new TransactionRequest<?>[] { request });

		return account;
	}

	@Override
	public StorageReference paidToLedgerBy(StorageReference payer, KeyPair keysOfPayer, PublicKey publicKey, BigInteger balance, Consumer<BigInteger> gasHandler, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, TimeoutException, InterruptedException, UnknownReferenceException, NoSuchAlgorithmException, UnsupportedVerificationVersionException, MisbehavingNodeException, ClosedNodeException, UnexpectedCodeException {

		// the ledger only uses ED25519 currently
		var signature = SignatureAlgorithms.ed25519();
		var signatureForPayer = SignatureHelpers.of(node).signatureAlgorithmFor(payer);

		BigInteger gas1 = gasForCreatingAccountWithSignature(signature);
		BigInteger gas2 = gasForTransactionWhosePayerHasSignature(signatureForPayer.getName());
		BigInteger totalGas = gas1.add(gas2).add(EXTRA_GAS_FOR_ANONYMOUS);

		gasHandler.accept(totalGas);

		String publicKeyEncoded = Base64.toBase64String(signature.encodingOf(publicKey));
		StorageReference account;
		Signer<SignedTransactionRequest<?>> signer = signatureForPayer.getSigner(keysOfPayer.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

		var accountsLedger = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_ACCOUNTS_LEDGER, manifest))
				.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.GET_ACCOUNTS_LEDGER))
				.asReturnedReference(MethodSignatures.GET_ACCOUNTS_LEDGER, UnexpectedValueException::new);

		var method = MethodSignatures.ofNonVoid(StorageTypes.ACCOUNTS_LEDGER, "add", StorageTypes.EOA, StorageTypes.BIG_INTEGER, StorageTypes.STRING);
		InstanceMethodCallTransactionRequest request = TransactionRequests.instanceMethodCall
				(signer, payer, nonceHelper.getNonceOf(payer),
						chainId, gas1.add(gas2).add(EXTRA_GAS_FOR_ANONYMOUS), gasHelper.getGasPrice(), takamakaCode,
						method,
						accountsLedger,
						StorageValues.bigIntegerOf(balance), StorageValues.stringOf(publicKeyEncoded));

		account = node.addInstanceMethodCallTransaction(request)
				.orElseThrow(() -> new UnexpectedVoidMethodException(method))
				.asReturnedReference(method, UnexpectedValueException::new);

		requestsHandler.accept(new TransactionRequest<?>[] { request });

		return account;
	}

	private static BigInteger gasForCreatingAccountWithSignature(SignatureAlgorithm signature) {
		switch (signature.getName()) {
		case "ed25519":
			return _100_000;
		case "sha256dsa":
			return BigInteger.valueOf(200_000L);
		case "qtesla1":
			return BigInteger.valueOf(3_000_000L);
		case "qtesla3":
			return BigInteger.valueOf(6_000_000L);
		default:
			LOGGER.warning("I do not how much gas to provide for creating an account with signature " + signature + ": using a default of " + _100_000);
			return _100_000;
		}
	}

	private static BigInteger gasForTransactionWhosePayerHasSignature(String signature) {
		switch (signature) {
		case "ed25519":
		case "sha256dsa":
			return _100_000;
		case "qtesla1":
			return BigInteger.valueOf(300_000L);
		case "qtesla3":
			return BigInteger.valueOf(400_000L);
		case "empty":
			return _100_000;
		default:
			LOGGER.warning("I do not how much gas to provide for creating an account with a payer with signature " + signature + ": using a default of " + _100_000);
			return _100_000;
		}
	}
}