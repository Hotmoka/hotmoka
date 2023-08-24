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
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.Signers;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.helpers.api.GasHelper;
import io.hotmoka.helpers.api.NonceHelper;
import io.hotmoka.helpers.api.SendCoinsHelper;
import io.hotmoka.nodes.SignatureAlgorithmForTransactionRequests;
import io.hotmoka.nodes.api.Node;

/**
 * Implementation of an object that helps with sending coins to accounts.
 */
public class SendCoinsHelperImpl implements SendCoinsHelper {
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
	public SendCoinsHelperImpl(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		this.node = node;
		this.manifest = node.getManifest();
		this.takamakaCode = node.getTakamakaCode();
		this.nonceHelper = NonceHelpers.of(node);
		this.gasHelper = GasHelpers.of(node);
		this.chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
	}

	@Override
	public void sendFromPayer(StorageReference payer, KeyPair keysOfPayer,
			StorageReference destination, BigInteger amount, BigInteger amountRed,
			Consumer<BigInteger> gasHandler, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException {

		SignatureAlgorithm<SignedTransactionRequest> signature = SignatureHelpers.of(node).signatureAlgorithmFor(payer);
		var signer = Signers.with(signature, keysOfPayer);
		BigInteger gas = gasForTransactionWhosePayerHasSignature(signature.getName(), node);
		BigInteger totalGas = amountRed.signum() > 0 ? gas.add(gas) : gas;
		gasHandler.accept(totalGas);

		var request1 = new InstanceMethodCallTransactionRequest
			(signer,
			payer, nonceHelper.getNonceOf(payer),
			chainId, gas, gasHelper.getGasPrice(), takamakaCode,
			CodeSignature.RECEIVE_BIG_INTEGER,
			destination,
			new BigIntegerValue(amount));

		node.addInstanceMethodCallTransaction(request1);
		requestsHandler.accept(new TransactionRequest<?>[] { request1 });

		if (amountRed.signum() > 0) {
			var request2 = new InstanceMethodCallTransactionRequest
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

	@Override
	public void sendFromFaucet(StorageReference destination, BigInteger amount, BigInteger amountRed,
			Consumer<BigInteger> gasHandler, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException {

		var gamete = (StorageReference) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(manifest, _100_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));

		gasHandler.accept(_100_000);

		// we use the empty signature algorithm, since the faucet is unsigned
		SignatureAlgorithm<SignedTransactionRequest> signature = SignatureAlgorithmForTransactionRequests.empty();
		var request = new InstanceMethodCallTransactionRequest
			(Signers.with(signature, signature.getKeyPair()),
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