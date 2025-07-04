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

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.helpers.api.AccountCreationHelper;
import io.hotmoka.helpers.api.GasHelper;
import io.hotmoka.helpers.api.NonceHelper;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.UnexpectedValueException;
import io.hotmoka.node.UnexpectedVoidMethodException;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnexpectedCodeException;
import io.hotmoka.node.api.UninitializedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;

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

	/**
	 * Creates an object that helps with the creation of new accounts.
	 * 
	 * @param node the node whose accounts are considered
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws ClosedNodeException if the node is already closed
	 * @throws CodeExecutionException 
	 * @throws TransactionException 
	 * @throws TransactionRejectedException 
	 * @throws UninitializedNodeException if the node is not initialized yet
	 * @throws UnexpectedCodeException if the Takamaka runtime is behaving in an unexpected way
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
	public StorageReference paidByFaucet(SignatureAlgorithm signatureAlgorithm, PublicKey publicKey,
			BigInteger balance, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, NodeException, InterruptedException, TimeoutException {

		StorageReference gamete = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest))
			.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.GET_GAMETE))
			.asReturnedReference(MethodSignatures.GET_GAMETE, UnexpectedValueException::new);

		String methodName;
		ClassType eoaType;
		BigInteger gas = gasForCreatingAccountWithSignature(signatureAlgorithm);

		String signature = signatureAlgorithm.getName();
		switch (signature) {
		case "ed25519":
		case "sha256dsa":
		case "qtesla1":
		case "qtesla3":
			methodName = "faucet" + signature.toUpperCase();
			eoaType = StorageTypes.classNamed(StorageTypes.EOA + signature.toUpperCase());
			break;
		default:
			throw new NodeException("Unknown signature algorithm " + signature);
		}

		// we use an empty signature algorithm and an arbitrary key, since the faucet is unsigned
		var signatureForFaucet = SignatureAlgorithms.empty();
		KeyPair keyPair = signatureForFaucet.getKeyPair();
		Signer<SignedTransactionRequest<?>> signer = signatureForFaucet.getSigner(keyPair.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);
		String publicKeyEncoded = Base64.toBase64String(signatureAlgorithm.encodingOf(publicKey));
		var method = MethodSignatures.ofNonVoid(StorageTypes.GAMETE, methodName, eoaType, StorageTypes.BIG_INTEGER, StorageTypes.STRING);
		BigInteger nonce;

		try {
			nonce = nonceHelper.getNonceOf(gamete);
		}
		catch (UnknownReferenceException e) {
			// the gamete exists and is an account
			throw new NodeException(e);
		}

		InstanceMethodCallTransactionRequest request;

		try {
			request = TransactionRequests.instanceMethodCall
				(signer, gamete, nonce, chainId, gas, gasHelper.getGasPrice(), takamakaCode,
				method, gamete, StorageValues.bigIntegerOf(balance), StorageValues.stringOf(publicKeyEncoded));
		}
		catch (SignatureException e) {
			// the empty signature never fails
			throw new NodeException(e);
		}

		return node.addInstanceMethodCallTransaction(request)
			.orElseThrow(() -> new UnexpectedVoidMethodException(method))
			.asReturnedReference(method, UnexpectedValueException::new);
	}

	@Override
	public StorageReference paidBy(StorageReference payer, KeyPair keysOfPayer,
			SignatureAlgorithm signatureAlgorithm, PublicKey publicKey, BigInteger balance,
			boolean addToLedger, Consumer<BigInteger> gasHandler, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException, UnknownReferenceException, NoSuchAlgorithmException {

		ClassType eoaType;
		String signature = signatureAlgorithm.getName();

		if (addToLedger && !"ed25519".equals(signature))
			throw new IllegalArgumentException("Can currently only store ed25519 accounts into the ledger of the manifest");	

		switch (signature) {
		case "ed25519":
		case "sha256dsa":
		case "qtesla1":
		case "qtesla3":
			eoaType = StorageTypes.classNamed(StorageTypes.EOA + signature.toUpperCase());
			break;
		default:
			throw new NodeException("Unknown signature algorithm " + signature);
		}

		var signatureForPayer = SignatureHelpers.of(node).signatureAlgorithmFor(payer);

		BigInteger gas1 = gasForCreatingAccountWithSignature(signatureAlgorithm);
		BigInteger gas2 = gasForTransactionWhosePayerHasSignature(signatureForPayer.getName());
		BigInteger totalGas = gas1.add(gas2);
		if (addToLedger)
			totalGas = totalGas.add(EXTRA_GAS_FOR_ANONYMOUS);

		gasHandler.accept(totalGas);

		String publicKeyEncoded = Base64.toBase64String(signatureAlgorithm.encodingOf(publicKey));
		StorageReference account;
		Signer<SignedTransactionRequest<?>> signer = signatureForPayer.getSigner(keysOfPayer.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

		if (addToLedger) {
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
		}
		else {
			ConstructorCallTransactionRequest request = TransactionRequests.constructorCall
				(signer, payer, nonceHelper.getNonceOf(payer),
				chainId, gas1.add(gas2), gasHelper.getGasPrice(), takamakaCode,
				ConstructorSignatures.of(eoaType, StorageTypes.BIG_INTEGER, StorageTypes.STRING),
				StorageValues.bigIntegerOf(balance), StorageValues.stringOf(publicKeyEncoded));

			account = node.addConstructorCallTransaction(request);
			requestsHandler.accept(new TransactionRequest<?>[] { request });
		}

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
			throw new IllegalArgumentException("Unknown signature algorithm " + signature);
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
			throw new IllegalArgumentException("Unknown signature algorithm " + signature);
		}
	}
}