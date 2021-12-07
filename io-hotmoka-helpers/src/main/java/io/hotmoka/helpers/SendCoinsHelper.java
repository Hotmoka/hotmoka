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

import static io.hotmoka.beans.types.ClassType.BIG_INTEGER;
import static io.hotmoka.beans.types.ClassType.GAMETE;
import static io.hotmoka.beans.types.ClassType.PAYABLE_CONTRACT;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.function.Consumer;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.SignatureAlgorithm;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests;
import io.hotmoka.nodes.Node;

/**
 * An object that helps with sending coins to accounts.
 */
public class SendCoinsHelper {
	private final Node node;
	private final StorageReference manifest;
	private final TransactionReference takamakaCode;
	private final NonceHelper nonceHelper;
	private final GasHelper gasHelper;
	private final String chainId;
	private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);

	/**
	 * Creates an object that helps with sending coins to accounts.
	 * 
	 * @param node the node whose accounts are considered
	 * @throws CodeExecutionException if some transaction fails
	 * @throws TransactionException if some transaction fails
	 * @throws TransactionRejectedException if some transaction fails
	 */
	public SendCoinsHelper(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		this.node = node;
		this.manifest = node.getManifest();
		this.takamakaCode = node.getTakamakaCode();
		this.nonceHelper = new NonceHelper(node);
		this.gasHelper = new GasHelper(node);
		this.chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
	}

	/**
	 * Sends coins to an account, by letting another account pay.
	 * 
	 * @param payer the sender of the coins
	 * @param keysOfPayer the keys of the {@code payer}
	 * @param destination the destination account
	 * @param amount the balance to transfer
	 * @param amountRed the red balance to transfer
	 * @param gasHandler a handler called with the total gas used for this operation. This can be useful for logging
	 * @param requestsHandler a handler called with the paid requests used for this operation. This can be useful for logging or computing costs
	 */
	public void fromPayer(StorageReference payer, KeyPair keysOfPayer,
			StorageReference destination, BigInteger amount, BigInteger amountRed,
			Consumer<BigInteger> gasHandler, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException {

		SignatureAlgorithm<SignedTransactionRequest> signature = new SignatureHelper(node).signatureAlgorithmFor(payer);
		Signer signer = Signer.with(signature, keysOfPayer);
		BigInteger gas = gasForTransactionWhosePayerHasSignature(signature.getName(), node);
		BigInteger totalGas = amountRed.signum() > 0 ? gas.add(gas) : gas;
		gasHandler.accept(totalGas);

		InstanceMethodCallTransactionRequest request1 = new InstanceMethodCallTransactionRequest
			(signer,
			payer, nonceHelper.getNonceOf(payer),
			chainId, gas, gasHelper.getGasPrice(), takamakaCode,
			CodeSignature.RECEIVE_BIG_INTEGER,
			destination,
			new BigIntegerValue(amount));

		node.addInstanceMethodCallTransaction(request1);
		requestsHandler.accept(new TransactionRequest<?>[] { request1 });

		if (amountRed.signum() > 0) {
			InstanceMethodCallTransactionRequest request2 = new InstanceMethodCallTransactionRequest
				(signer,
				payer, nonceHelper.getNonceOf(payer),
				chainId, gas, gasHelper.getGasPrice(), takamakaCode,
				CodeSignature.RECEIVE_RED_BIG_INTEGER,
				destination,
				new BigIntegerValue(amountRed));

			node.addInstanceMethodCallTransaction(request2);
			requestsHandler.accept(new TransactionRequest<?>[] { request2 });
		}
	}

	/**
	 * Sends coins to an account, by letting the faucet of the node pay.
	 * 
	 * @param destination the destination account
	 * @param amount the balance to transfer
	 * @param amountRed the red balance to transfer
	 * @param gasHandler a handler called with the total gas used for this operation. This can be useful for logging
	 * @param requestsHandler a handler called with the paid requests used for this operation. This can be useful for logging or computing costs
	 */
	public void fromFaucet(StorageReference destination, BigInteger amount, BigInteger amountRed,
			Consumer<BigInteger> gasHandler, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException {

		StorageReference gamete = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));

		gasHandler.accept(_100_000);

		// we use the empty signature algorithm, since the faucet is unsigned
		SignatureAlgorithm<SignedTransactionRequest> signature = SignatureAlgorithmForTransactionRequests.empty();
		InstanceMethodCallTransactionRequest request = new InstanceMethodCallTransactionRequest
			(Signer.with(signature, signature.getKeyPair()),
			gamete, nonceHelper.getNonceOf(gamete),
			chainId, _100_000, gasHelper.getGasPrice(), takamakaCode,
			new VoidMethodSignature(GAMETE, "faucet", PAYABLE_CONTRACT, BIG_INTEGER, BIG_INTEGER),
			gamete,
			destination, new BigIntegerValue(amount), new BigIntegerValue(amountRed));

		node.addInstanceMethodCallTransaction(request);
		requestsHandler.accept(new TransactionRequest<?>[] { request });
	}

	private static BigInteger gasForTransactionWhosePayerHasSignature(String signature, Node node) {
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