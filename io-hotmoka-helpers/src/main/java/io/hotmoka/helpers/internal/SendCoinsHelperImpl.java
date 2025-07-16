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
import java.util.logging.Logger;

import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.helpers.NonceHelpers;
import io.hotmoka.helpers.SignatureHelpers;
import io.hotmoka.helpers.UnexpectedValueException;
import io.hotmoka.helpers.UnexpectedVoidMethodException;
import io.hotmoka.helpers.api.GasHelper;
import io.hotmoka.helpers.api.MisbehavingNodeException;
import io.hotmoka.helpers.api.NonceHelper;
import io.hotmoka.helpers.api.SendCoinsHelper;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnexpectedCodeException;
import io.hotmoka.node.api.UninitializedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;

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
	private final static Logger LOGGER = Logger.getLogger(SendCoinsHelperImpl.class.getName());

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
	 * @throws UnexpectedCodeException if the Takamaka runtime is behaving in an unexpected way
	 */
	public SendCoinsHelperImpl(Node node) throws ClosedNodeException, TimeoutException, InterruptedException, TransactionRejectedException, TransactionException, CodeExecutionException, UninitializedNodeException, UnexpectedCodeException {
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
			throws TransactionRejectedException, TransactionException, InvalidKeyException, SignatureException, TimeoutException, InterruptedException, UnknownReferenceException, CodeExecutionException, NoSuchAlgorithmException, UnsupportedVerificationVersionException, ClosedNodeException, UnexpectedCodeException, MisbehavingNodeException {

		var signature = SignatureHelpers.of(node).signatureAlgorithmFor(payer);
		Signer<SignedTransactionRequest<?>> signer = signature.getSigner(keysOfPayer.getPrivate(), SignedTransactionRequest::toByteArrayWithoutSignature);
		BigInteger gas = gasForTransactionWhosePayerHasSignature(signature.getName());
		gasHandler.accept(gas);

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
			throws TransactionRejectedException, TransactionException, InterruptedException, TimeoutException, CodeExecutionException, ClosedNodeException, UnexpectedCodeException, MisbehavingNodeException {

		try {
			var gamete = node.runInstanceMethodCallTransaction(TransactionRequests.instanceViewMethodCall
					(manifest, _100_000, takamakaCode, MethodSignatures.GET_GAMETE, manifest))
					.orElseThrow(() -> new UnexpectedVoidMethodException(MethodSignatures.GET_GAMETE))
					.asReturnedReference(MethodSignatures.GET_GAMETE, UnexpectedValueException::new);

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
			throw new RuntimeException("Unexpected exception from the empty signature algorithm", e);
		}
		catch (UnknownReferenceException e) {
			throw new MisbehavingNodeException("The node does not contain its same gamete", e);
		}
	}

	private static BigInteger gasForTransactionWhosePayerHasSignature(String signature) {
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
			LOGGER.warning("I do not how much gas to provide for sending coins from an account with signature " + signature + ": using a default of " + _100_000);
			return _100_000;
		}
	}
}