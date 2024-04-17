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
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionRequests;
import io.hotmoka.beans.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.api.requests.SignedTransactionRequest;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StringValue;
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
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

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
	 * @throws CodeExecutionException if some transaction fails
	 * @throws TransactionException if some transaction fails
	 * @throws TransactionRejectedException if some transaction fails
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws NodeException if the node is not able to complete the operation
	 */
	public AccountCreationHelperImpl(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException {
		this.node = node;
		this.manifest = node.getManifest();
		this.takamakaCode = node.getTakamakaCode();
		this.nonceHelper = NonceHelpers.of(node);
		this.gasHelper = GasHelpers.of(node);
		this.chainId = ((StringValue) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.GET_CHAIN_ID, manifest))).getValue();
	}

	@Override
	public StorageReference paidByFaucet(SignatureAlgorithm signatureAlgorithm, PublicKey publicKey,
			BigInteger balance, BigInteger balanceRed, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchElementException, NodeException, InterruptedException, TimeoutException {

		var gamete = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest));

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
			throw new IllegalArgumentException("Unknown signature algorithm " + signature);
		}

		// we use an empty signature algorithm and an arbitrary key, since the faucet is unsigned
		var signatureForFaucet = SignatureAlgorithms.empty();
		KeyPair keyPair = signatureForFaucet.getKeyPair();
		Signer<SignedTransactionRequest<?>> signer = signatureForFaucet.getSigner(keyPair.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);
		String publicKeyEncoded = Base64.toBase64String(signatureAlgorithm.encodingOf(publicKey));
		var request = TransactionRequests.instanceMethodCall
			(signer, gamete, nonceHelper.getNonceOf(gamete),
			chainId, gas, gasHelper.getGasPrice(), takamakaCode,
			MethodSignatures.of(StorageTypes.GAMETE, methodName, eoaType, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.STRING),
			gamete,
			StorageValues.bigIntegerOf(balance), StorageValues.bigIntegerOf(balanceRed), StorageValues.stringOf(publicKeyEncoded));

		return (StorageReference) node.addInstanceMethodCallTransaction(request);
	}

	@Override
	public StorageReference paidBy(StorageReference payer, KeyPair keysOfPayer,
			SignatureAlgorithm signatureAlgorithm, PublicKey publicKey, BigInteger balance, BigInteger balanceRed,
			boolean addToLedger,
			Consumer<BigInteger> gasHandler,
			Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {

		ClassType eoaType;
		String signature = signatureAlgorithm.getName();

		if (addToLedger && !"ed25519".equals(signature))
			throw new IllegalArgumentException("can only store ed25519 accounts into the ledger of the manifest");	

		switch (signature) {
		case "ed25519":
		case "sha256dsa":
		case "qtesla1":
		case "qtesla3":
			eoaType = StorageTypes.classNamed(StorageTypes.EOA + signature.toUpperCase());
			break;
		default:
			throw new IllegalArgumentException("unknown signature algorithm " + signature);
		}

		var signatureForPayer = SignatureHelpers.of(node).signatureAlgorithmFor(payer);

		BigInteger gas1 = gasForCreatingAccountWithSignature(signatureAlgorithm);
		BigInteger gas2 = gasForTransactionWhosePayerHasSignature(signatureForPayer.getName());
		BigInteger totalGas = balanceRed.signum() > 0 ? gas1.add(gas2).add(gas2) : gas1.add(gas2);
		if (addToLedger)
			totalGas = totalGas.add(EXTRA_GAS_FOR_ANONYMOUS);

		gasHandler.accept(totalGas);

		String publicKeyEncoded = Base64.toBase64String(signatureAlgorithm.encodingOf(publicKey));
		StorageReference account;
		TransactionRequest<?> request1;
		Signer<SignedTransactionRequest<?>> signer = signatureForPayer.getSigner(keysOfPayer.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);

		if (addToLedger) {
			var accountsLedger = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
				(manifest, _100_000, takamakaCode, MethodSignatures.GET_ACCOUNTS_LEDGER, manifest));

			request1 = TransactionRequests.instanceMethodCall
				(signer, payer, nonceHelper.getNonceOf(payer),
				chainId, gas1.add(gas2).add(EXTRA_GAS_FOR_ANONYMOUS), gasHelper.getGasPrice(), takamakaCode,
				MethodSignatures.of(StorageTypes.ACCOUNTS_LEDGER, "add", StorageTypes.EOA, StorageTypes.BIG_INTEGER, StorageTypes.STRING),
				accountsLedger,
				StorageValues.bigIntegerOf(balance),
				StorageValues.stringOf(publicKeyEncoded));

			account = (StorageReference) node.addInstanceMethodCallTransaction((InstanceMethodCallTransactionRequest) request1);
		}
		else {
			request1 = TransactionRequests.constructorCall
				(signer, payer, nonceHelper.getNonceOf(payer),
				chainId, gas1.add(gas2), gasHelper.getGasPrice(), takamakaCode,
				ConstructorSignatures.of(eoaType, StorageTypes.BIG_INTEGER, StorageTypes.STRING),
				StorageValues.bigIntegerOf(balance), StorageValues.stringOf(publicKeyEncoded));
			account = node.addConstructorCallTransaction((ConstructorCallTransactionRequest) request1);
		}

		if (balanceRed.signum() > 0) {
			var request2 = TransactionRequests.instanceMethodCall
				(signer, payer, nonceHelper.getNonceOf(payer), chainId, gas2, gasHelper.getGasPrice(), takamakaCode,
						MethodSignatures.RECEIVE_RED_BIG_INTEGER, account, StorageValues.bigIntegerOf(balanceRed));
			node.addInstanceMethodCallTransaction(request2);
			
			requestsHandler.accept(new TransactionRequest<?>[] { request1, request2 });
		}
		else
			requestsHandler.accept(new TransactionRequest<?>[] { request1 });

		return account;
	}

	@Override
	public StorageReference tendermintValidatorPaidByFaucet(PublicKey publicKey,
			BigInteger balance, BigInteger balanceRed, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, NoSuchElementException, NodeException, InterruptedException, TimeoutException {

		var gamete = (StorageReference) node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
			(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest));

		var ed25519 = SignatureAlgorithms.ed25519();
		BigInteger gas = gasForCreatingAccountWithSignature(ed25519);

		// we use an empty signature algorithm and an arbitrary key, since the faucet is unsigned
		var signatureForFaucet = SignatureAlgorithms.empty();
		KeyPair keyPair = signatureForFaucet.getKeyPair();
		Signer<SignedTransactionRequest<?>> signer = signatureForFaucet.getSigner(keyPair.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);
		String publicKeyEncoded = Base64.toBase64String(ed25519.encodingOf(publicKey)); // Tendermint uses ed25519 only
		var request = TransactionRequests.instanceMethodCall
			(signer, gamete, nonceHelper.getNonceOf(gamete),
			chainId, gas, gasHelper.getGasPrice(), takamakaCode,
			MethodSignatures.of(StorageTypes.GAMETE, "faucetTendermintED25519Validator", StorageTypes.TENDERMINT_ED25519_VALIDATOR, StorageTypes.BIG_INTEGER, StorageTypes.BIG_INTEGER, StorageTypes.STRING),
			gamete,
			StorageValues.bigIntegerOf(balance), StorageValues.bigIntegerOf(balanceRed), StorageValues.stringOf(publicKeyEncoded));

		return (StorageReference) node.addInstanceMethodCallTransaction(request);
	}

	@Override
	public StorageReference tendermintValidatorPaidBy(StorageReference payer, KeyPair keysOfPayer, PublicKey publicKey, BigInteger balance, BigInteger balanceRed,
			Consumer<BigInteger> gasHandler,
			Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {

		var signatureForPayer = SignatureHelpers.of(node).signatureAlgorithmFor(payer);

		var ed25519 = SignatureAlgorithms.ed25519();
		BigInteger gas1 = gasForCreatingAccountWithSignature(ed25519);
		BigInteger gas2 = gasForTransactionWhosePayerHasSignature(signatureForPayer.getName());
		BigInteger totalGas = balanceRed.signum() > 0 ? gas1.add(gas2).add(gas2) : gas1.add(gas2);

		gasHandler.accept(totalGas);

		Signer<SignedTransactionRequest<?>> signer = signatureForPayer.getSigner(keysOfPayer.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);
		String publicKeyEncoded = Base64.toBase64String(ed25519.encodingOf(publicKey)); // Tendermint uses ed25519 only
		var request1 = TransactionRequests.constructorCall
			(signer, payer, nonceHelper.getNonceOf(payer),
			chainId, gas1.add(gas2), gasHelper.getGasPrice(), takamakaCode,
			ConstructorSignatures.of(StorageTypes.TENDERMINT_ED25519_VALIDATOR, StorageTypes.BIG_INTEGER, StorageTypes.STRING),
			StorageValues.bigIntegerOf(balance), StorageValues.stringOf(publicKeyEncoded));
		StorageReference validator = node.addConstructorCallTransaction(request1);

		if (balanceRed.signum() > 0) {
			var request2 = TransactionRequests.instanceMethodCall
				(signer, payer, nonceHelper.getNonceOf(payer), chainId, gas2, gasHelper.getGasPrice(), takamakaCode,
						MethodSignatures.RECEIVE_RED_BIG_INTEGER, validator, StorageValues.bigIntegerOf(balanceRed));
			node.addInstanceMethodCallTransaction(request2);
			
			requestsHandler.accept(new TransactionRequest<?>[] { request1, request2 });
		}
		else
			requestsHandler.accept(new TransactionRequest<?>[] { request1 });

		return validator;
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
			throw new IllegalArgumentException("unknown signature algorithm " + signature);
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
			throw new IllegalArgumentException("unknown signature algorithm " + signature);
		}
	}
}