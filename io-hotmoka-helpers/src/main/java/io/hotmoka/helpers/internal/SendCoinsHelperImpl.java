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

import static io.hotmoka.node.StorageTypes.BIG_INTEGER;
import static io.hotmoka.node.StorageTypes.GAMETE;
import static io.hotmoka.node.StorageTypes.PAYABLE_CONTRACT;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.helpers.api.GasHelper;
import io.hotmoka.helpers.api.NonceHelper;
import io.hotmoka.helpers.api.SendCoinsHelper;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.UnexpectedVoidMethodException;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UninitializedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;

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
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws ClosedNodeException if the node is already closed
	 * @throws CodeExecutionException if some transaction threw an exception
	 * @throws TransactionException if some transaction failed
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws UninitializedNodeException if the node is not initialized yet
	 */
	public SendCoinsHelperImpl(Node node) throws ClosedNodeException, TimeoutException, InterruptedException, TransactionRejectedException, TransactionException, CodeExecutionException, UninitializedNodeException {
		this.node = node;
		this.manifest = node.getManifest();
		this.takamakaCode = node.getTakamakaCode();
		this.nonceHelper = NonceHelpers.of(node);
		this.gasHelper = GasHelpers.of(node);
		this.chainId = node.getConfig().getChainId();
	}

	@Override
	public void sendFromPayer(StorageReference payer, KeyPair keysOfPayer,
			StorageReference destination, BigInteger amount,
			Consumer<BigInteger> gasHandler, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException, UnknownReferenceException, CodeExecutionException, NoSuchAlgorithmException {

		var signature = SignatureHelpers.of(node).signatureAlgorithmFor(payer);
		Signer<SignedTransactionRequest<?>> signer = signature.getSigner(keysOfPayer.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);
		BigInteger gas = gasForTransactionWhosePayerHasSignature(signature.getName(), node);
		BigInteger totalGas = gas;
		gasHandler.accept(totalGas);

		var request = TransactionRequests.instanceMethodCall
			(signer,
			payer, nonceHelper.getNonceOf(payer),
			chainId, gas, gasHelper.getGasPrice(), takamakaCode,
			MethodSignatures.RECEIVE_BIG_INTEGER,
			destination,
			StorageValues.bigIntegerOf(amount));

		node.addInstanceMethodCallTransaction(request);
		requestsHandler.accept(new TransactionRequest<?>[] { request });
	}

	@Override
	public void sendFromFaucet(StorageReference destination, BigInteger amount,
			Consumer<BigInteger> gasHandler, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, NodeException, InterruptedException, TimeoutException, CodeExecutionException {

		try {
			var gamete = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest))
					.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.GET_GAMETE))
					.asReturnedReference(MethodSignatures.GET_GAMETE, NodeException::new);

			gasHandler.accept(_100_000);

			// we use the empty signature algorithm, since the faucet is unsigned
			var signature = SignatureAlgorithms.empty();
			var request = TransactionRequests.instanceMethodCall
				(signature.getSigner(signature.getKeyPair().getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature),
				gamete, nonceHelper.getNonceOf(gamete),
				chainId, _100_000, gasHelper.getGasPrice(), takamakaCode,
				MethodSignatures.ofVoid(GAMETE, "faucet", PAYABLE_CONTRACT, BIG_INTEGER),
				gamete, destination, StorageValues.bigIntegerOf(amount));

			node.addInstanceMethodCallTransaction(request);
			requestsHandler.accept(new TransactionRequest<?>[] { request });
		}
		catch (InvalidKeyException | SignatureException e) {
			// the empty signature does not throw these
			throw new NodeException(e);
		}
		catch (UnknownReferenceException e) {
			// the gamete of the node must exist, since the node is initialized
			throw new NodeException(e);
		}
	}

	private static BigInteger gasForTransactionWhosePayerHasSignature(String signature, Node node) throws NodeException {
		switch (signature) {
		case "ed25519":
		case "sha256dsa":
		case "empty":
			return _100_000;
		case "qtesla1":
			return BigInteger.valueOf(300_000L);
		case "qtesla3":
			return BigInteger.valueOf(400_000L);
		default:
			throw new NodeException("Unknown signature algorithm " + signature);
		}
	}
}