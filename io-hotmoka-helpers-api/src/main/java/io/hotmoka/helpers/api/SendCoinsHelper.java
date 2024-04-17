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
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;

/**
 * An object that helps with sending coins to accounts.
 */
@ThreadSafe
public interface SendCoinsHelper {

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
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 * @throws ClassNotFoundException if some class of the Takamaka runtime cannot be loaded
	 * @throws InvalidKeyException if the key is invalid
	 * @throws SignatureException if some signature failed
	 * @throws NoSuchAlgorithmException if the sender uses an unknown signature algorithm
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws NodeException if the node is not able to complete the operation
	 * @throws UnknownReferenceException if the node is not properly initialized
	 */
	void sendFromPayer(StorageReference payer, KeyPair keysOfPayer,
			StorageReference destination, BigInteger amount, BigInteger amountRed,
			Consumer<BigInteger> gasHandler, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException,
				NodeException, UnknownReferenceException, TimeoutException, InterruptedException;

	/**
	 * Sends coins to an account, by letting the faucet of the node pay.
	 * 
	 * @param destination the destination account
	 * @param amount the balance to transfer
	 * @param amountRed the red balance to transfer
	 * @param gasHandler a handler called with the total gas used for this operation. This can be useful for logging
	 * @param requestsHandler a handler called with the paid requests used for this operation. This can be useful for logging or computing costs
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 * @throws ClassNotFoundException if some class of the Takamaka runtime cannot be loaded
	 * @throws InvalidKeyException if the key is invalid
	 * @throws SignatureException if some signature failed
	 * @throws NoSuchAlgorithmException if the faucet uses an unknown signature algorithm
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws InterruptedException if the current thread gets interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 */
	void sendFromFaucet(StorageReference destination, BigInteger amount, BigInteger amountRed,
			Consumer<BigInteger> gasHandler, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException, NodeException,
				InterruptedException, TimeoutException, UnknownReferenceException;
}