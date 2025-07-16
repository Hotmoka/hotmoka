/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.helpers.api;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnexpectedCodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;

/**
 * An object that helps with sending coins to accounts.
 */
@ThreadSafe
public interface SendCoinsHelper { // TODO: this is only used by the Android client: share the code with moka to avoid duplication

	/**
	 * Sends coins to an account, by letting another account pay.
	 * 
	 * @param payer the sender of the coins
	 * @param keysOfPayer the keys of the {@code payer}
	 * @param destination the destination account
	 * @param amount the balance to transfer
	 * @param gasHandler a handler called with the total gas used for this operation. This can be useful for logging
	 * @param requestsHandler a handler called with the paid requests used for this operation. This can be useful for logging or computing costs
	 * @throws TransactionRejectedException if some transaction gets rejected
	 * @throws TransactionException if some transaction fails
	 * @throws CodeExecutionException if some transaction throws an exception
	 * @throws InvalidKeyException if {@code keysOfPayer} is invalid
	 * @throws SignatureException if signing with {@code keysOfPayer} failed
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws UnknownReferenceException if {@code payer} cannot be found in the store of the node
	 * @throws NoSuchAlgorithmException if the signature algorithm of {@code payer} is not available
	 * @throws UnsupportedVerificationVersionException if the node uses a verification version that is not available
	 * @throws ClosedNodeException if the node is already closed
	 * @throws UnexpectedCodeException if the Takamaka runtime installed in the node is not as expected
	 * @throws MisbehavingNodeException if the node is behaving in a buggy way
	 */
	void sendFromPayer(StorageReference payer, KeyPair keysOfPayer, StorageReference destination, BigInteger amount,
			Consumer<BigInteger> gasHandler, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, InvalidKeyException, SignatureException, TimeoutException, InterruptedException, UnknownReferenceException, CodeExecutionException, NoSuchAlgorithmException, UnsupportedVerificationVersionException, ClosedNodeException, UnexpectedCodeException, MisbehavingNodeException;

	/**
	 * Sends coins to an account, by letting the faucet of the node pay.
	 * 
	 * @param destination the destination account
	 * @param amount the balance to transfer
	 * @param gasHandler a handler called with the total gas used for this operation. This can be useful for logging
	 * @param requestsHandler a handler called with the paid requests used for this operation. This can be useful for logging or computing costs
	 * @throws TransactionRejectedException if some transaction gets rejected
	 * @throws TransactionException if some transaction fails
	 * @throws CodeExecutionException if some transaction throws an exception
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws ClosedNodeException if the node is already closed
	 * @throws UnexpectedCodeException if the Takamaka runtime installed in the node is not as expected
	 * @throws MisbehavingNodeException if the node is behaving in a buggy way
	 */
	void sendFromFaucet(StorageReference destination, BigInteger amount, Consumer<BigInteger> gasHandler, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, InterruptedException, TimeoutException, CodeExecutionException, ClosedNodeException, UnexpectedCodeException, MisbehavingNodeException;
}