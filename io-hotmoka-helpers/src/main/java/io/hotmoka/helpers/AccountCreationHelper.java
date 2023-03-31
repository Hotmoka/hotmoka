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

package io.hotmoka.helpers;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Base64;
import java.util.function.Consumer;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.SignatureAlgorithm;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.SignatureAlgorithmForTransactionRequests;

/**
 * An object that helps with the creation of new accounts.
 */
public class AccountCreationHelper {
	private final Node node;
	private final StorageReference manifest;
	private final TransactionReference takamakaCode;
	private final NonceHelper nonceHelper;
	private final GasHelper gasHelper;
	private final String chainId;
	private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);

	/**
	 * The extra gas cost for paying to a public key in anonymous way, hence
	 * storing the new account in the account ledger of the node.
	 */
	public final static BigInteger EXTRA_GAS_FOR_ANONYMOUS = BigInteger.valueOf(500_000L);

	/**
	 * Creates an object that helps with the creation of new accounts.
	 * 
	 * @param node the node whose accounts are considered
	 * @throws CodeExecutionException if some transaction fails
	 * @throws TransactionException if some transaction fails
	 * @throws TransactionRejectedException if some transaction fails
	 */
	public AccountCreationHelper(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		this.node = node;
		this.manifest = node.getManifest();
		this.takamakaCode = node.getTakamakaCode();
		this.nonceHelper = new NonceHelper(node);
		this.gasHelper = new GasHelper(node);
		this.chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
	}

	/**
	 * Creates a new account by letting the faucet pay.
	 * 
	 * @param signatureAlgorithm the signature algorithm for the new account
	 * @param publicKey the public key of the new account
	 * @param balance the balance of the new account
	 * @param balanceRed the red balance of the new account
	 * @param requestsHandler a handler called with the paid requests used for this operation. This can be useful for logging or computing costs
	 * @return the storage reference of the account
	 */
	public StorageReference fromFaucet(SignatureAlgorithm<SignedTransactionRequest> signatureAlgorithm, PublicKey publicKey,
			BigInteger balance, BigInteger balanceRed, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException {

		StorageReference gamete = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));

		String methodName;
		ClassType eoaType;
		String signature = signatureAlgorithm.getName();
		BigInteger gas = gasForCreatingAccountWithSignature(signature);

		switch (signature) {
		case "ed25519":
		case "sha256dsa":
		case "qtesla1":
		case "qtesla3":
			methodName = "faucet" + signature.toUpperCase();
			eoaType = new ClassType(ClassType.EOA.name + signature.toUpperCase());
			break;
		default:
			throw new IllegalArgumentException("unknown signature algorithm " + signature);
		}

		// we use an empty signature algorithm and an arbitrary key, since the faucet is unsigned
		SignatureAlgorithm<SignedTransactionRequest> signatureForFaucet = SignatureAlgorithmForTransactionRequests.empty();
		KeyPair keyPair = signatureForFaucet.getKeyPair();
		Signer signer = Signer.with(signatureForFaucet, keyPair);
		String publicKeyEncoded = Base64.getEncoder().encodeToString(signatureAlgorithm.encodingOf(publicKey));
		InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest
			(signer, gamete, nonceHelper.getNonceOf(gamete),
			chainId, gas, gasHelper.getGasPrice(), takamakaCode,
			new NonVoidMethodSignature(ClassType.GAMETE, methodName, eoaType, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, ClassType.STRING),
			gamete,
			new BigIntegerValue(balance), new BigIntegerValue(balanceRed), new StringValue(publicKeyEncoded));

		return (StorageReference) node.addInstanceMethodCallTransaction(request);
	}

	/**
	 * Creates a new account by letting another account pay.
	 * 
	 * @param payer the payer of the account creation
	 * @param keysOfPayer the keys of the {@code payer}
	 * @param signatureAlgorithm the signature algorithm for the new account
	 * @param publicKey the public key of the new account
	 * @param balance the balance of the new account
	 * @param balanceRed the red balance of the new account
	 * @param addToLedger adds the new account to the ledger of the manifest, bound to its {@code publicKey}; if an account already
	 *                    exists for {@code publicKey}, that account gets funded with {@code balance} and {@code balanceRed} coins and returned
	 * @param gasHandler a handler called with the total gas used for this operation. This can be useful for logging
	 * @param requestsHandler a handler called with the paid requests used for this operation. This can be useful for logging or computing costs
	 * @return the storage reference of the account
	 */
	public StorageReference fromPayer(StorageReference payer, KeyPair keysOfPayer,
			SignatureAlgorithm<SignedTransactionRequest> signatureAlgorithm, PublicKey publicKey, BigInteger balance, BigInteger balanceRed,
			boolean addToLedger,
			Consumer<BigInteger> gasHandler,
			Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException {

		ClassType eoaType;
		String signature = signatureAlgorithm.getName();

		if (addToLedger && !"ed25519".equals(signature))
			throw new IllegalArgumentException("can only store ed25519 accounts into the ledger of the manifest");	

		switch (signature) {
		case "ed25519":
		case "sha256dsa":
		case "qtesla1":
		case "qtesla3":
			eoaType = new ClassType(ClassType.EOA.name + signature.toUpperCase());
			break;
		default:
			throw new IllegalArgumentException("unknown signature algorithm " + signature);
		}

		SignatureAlgorithm<SignedTransactionRequest> signatureForPayer = new SignatureHelper(node).signatureAlgorithmFor(payer);

		BigInteger gas1 = gasForCreatingAccountWithSignature(signature);
		BigInteger gas2 = gasForTransactionWhosePayerHasSignature(signatureForPayer.getName());
		BigInteger totalGas = balanceRed.signum() > 0 ? gas1.add(gas2).add(gas2) : gas1.add(gas2);
		if (addToLedger)
			totalGas = totalGas.add(EXTRA_GAS_FOR_ANONYMOUS);

		gasHandler.accept(totalGas);

		Signer signer = Signer.with(signatureForPayer, keysOfPayer);
		String publicKeyEncoded = Base64.getEncoder().encodeToString(signatureAlgorithm.encodingOf(publicKey));
		StorageReference account;
		TransactionRequest<?> request1;

		if (addToLedger) {
			StorageReference accountsLedger = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _100_000, takamakaCode, CodeSignature.GET_ACCOUNTS_LEDGER, manifest));

			request1 = new InstanceMethodCallTransactionRequest
				(signer, payer, nonceHelper.getNonceOf(payer),
				chainId, gas1.add(gas2).add(EXTRA_GAS_FOR_ANONYMOUS), gasHelper.getGasPrice(), takamakaCode,
				new NonVoidMethodSignature(ClassType.ACCOUNTS_LEDGER, "add", ClassType.EOA, ClassType.BIG_INTEGER, ClassType.STRING),
				accountsLedger,
				new BigIntegerValue(balance),
				new StringValue(publicKeyEncoded));

			account = (StorageReference) node.addInstanceMethodCallTransaction((InstanceMethodCallTransactionRequest) request1);
		}
		else {
			request1 = new ConstructorCallTransactionRequest
				(signer, payer, nonceHelper.getNonceOf(payer),
				chainId, gas1.add(gas2), gasHelper.getGasPrice(), takamakaCode,
				new ConstructorSignature(eoaType, ClassType.BIG_INTEGER, ClassType.STRING),
				new BigIntegerValue(balance), new StringValue(publicKeyEncoded));
			account = node.addConstructorCallTransaction((ConstructorCallTransactionRequest) request1);
		}

		if (balanceRed.signum() > 0) {
			InstanceMethodCallTransactionRequest request2 = new InstanceMethodCallTransactionRequest
				(signer, payer, nonceHelper.getNonceOf(payer), chainId, gas2, gasHelper.getGasPrice(), takamakaCode,
				CodeSignature.RECEIVE_RED_BIG_INTEGER, account, new BigIntegerValue(balanceRed));
			node.addInstanceMethodCallTransaction(request2);
			
			requestsHandler.accept(new TransactionRequest<?>[] { request1, request2 });
		}
		else
			requestsHandler.accept(new TransactionRequest<?>[] { request1 });

		return account;
	}

	/**
	 * Creates a new Tendermint validator by letting the faucet pay.
	 * 
	 * @param publicKey the public key of the new validator
	 * @param balance the balance of the new validator
	 * @param balanceRed the red balance of the new validator
	 * @param requestsHandler a handler called with the paid requests used for this operation. This can be useful for logging or computing costs
	 * @return the storage reference of the validator
	 */
	public StorageReference tendermintValidatorFromFaucet(PublicKey publicKey,
			BigInteger balance, BigInteger balanceRed, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {

		StorageReference gamete = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));

		BigInteger gas = gasForCreatingAccountWithSignature("ed25519");

		// we use an empty signature algorithm and an arbitrary key, since the faucet is unsigned
		SignatureAlgorithm<SignedTransactionRequest> signatureForFaucet = SignatureAlgorithmForTransactionRequests.empty();
		KeyPair keyPair = signatureForFaucet.getKeyPair();
		Signer signer = Signer.with(signatureForFaucet, keyPair);
		String publicKeyEncoded = Base64.getEncoder().encodeToString(SignatureAlgorithmForTransactionRequests.ed25519().encodingOf(publicKey));
		InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest
			(signer, gamete, nonceHelper.getNonceOf(gamete),
			chainId, gas, gasHelper.getGasPrice(), takamakaCode,
			new NonVoidMethodSignature(ClassType.GAMETE, "faucetTendermintED25519Validator", ClassType.TENDERMINT_ED25519_VALIDATOR, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, ClassType.STRING),
			gamete,
			new BigIntegerValue(balance), new BigIntegerValue(balanceRed), new StringValue(publicKeyEncoded));

		return (StorageReference) node.addInstanceMethodCallTransaction(request);
	}

	/**
	 * Creates a new Tendermint validator by letting another account pay.
	 * 
	 * @param payer the payer of the validator creation
	 * @param keysOfPayer the keys of the {@code payer}
	 * @param publicKey the public key of the new validator
	 * @param balance the balance of the new validator
	 * @param balanceRed the red balance of the new validator
	 * @param gasHandler a handler called with the total gas used for this operation. This can be useful for logging
	 * @param requestsHandler a handler called with the paid requests used for this operation. This can be useful for logging or computing costs
	 * @return the storage reference of the new validator
	 */
	public StorageReference tendermintValidatorFromPayer(StorageReference payer, KeyPair keysOfPayer, PublicKey publicKey, BigInteger balance, BigInteger balanceRed,
			Consumer<BigInteger> gasHandler,
			Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException {

		SignatureAlgorithm<SignedTransactionRequest> signatureForPayer = new SignatureHelper(node).signatureAlgorithmFor(payer);

		BigInteger gas1 = gasForCreatingAccountWithSignature("ed25519");
		BigInteger gas2 = gasForTransactionWhosePayerHasSignature(signatureForPayer.getName());
		BigInteger totalGas = balanceRed.signum() > 0 ? gas1.add(gas2).add(gas2) : gas1.add(gas2);

		gasHandler.accept(totalGas);

		Signer signer = Signer.with(signatureForPayer, keysOfPayer);
		String publicKeyEncoded = Base64.getEncoder().encodeToString(SignatureAlgorithmForTransactionRequests.ed25519().encodingOf(publicKey));
		TransactionRequest<?> request1 = new ConstructorCallTransactionRequest
			(signer, payer, nonceHelper.getNonceOf(payer),
			chainId, gas1.add(gas2), gasHelper.getGasPrice(), takamakaCode,
			new ConstructorSignature(ClassType.TENDERMINT_ED25519_VALIDATOR, ClassType.BIG_INTEGER, ClassType.STRING),
			new BigIntegerValue(balance), new StringValue(publicKeyEncoded));
		StorageReference validator = node.addConstructorCallTransaction((ConstructorCallTransactionRequest) request1);

		if (balanceRed.signum() > 0) {
			InstanceMethodCallTransactionRequest request2 = new InstanceMethodCallTransactionRequest
				(signer, payer, nonceHelper.getNonceOf(payer), chainId, gas2, gasHelper.getGasPrice(), takamakaCode,
				CodeSignature.RECEIVE_RED_BIG_INTEGER, validator, new BigIntegerValue(balanceRed));
			node.addInstanceMethodCallTransaction(request2);
			
			requestsHandler.accept(new TransactionRequest<?>[] { request1, request2 });
		}
		else
			requestsHandler.accept(new TransactionRequest<?>[] { request1 });

		return validator;
	}

	private BigInteger gasForCreatingAccountWithSignature(String signature) {
		switch (signature) {
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

	private BigInteger gasForTransactionWhosePayerHasSignature(String signature) {
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