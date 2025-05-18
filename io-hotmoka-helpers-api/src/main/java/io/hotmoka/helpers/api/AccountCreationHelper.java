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
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.values.StorageReference;

/**
 * An object that helps with the creation of new accounts.
 */
@ThreadSafe
public interface AccountCreationHelper { // TODO: this is only used by the Android client: share the code with moka to avoid duplication

	/**
	 * The extra gas cost for paying to a public key in anonymous way, hence
	 * storing the new account in the account ledger of the node.
	 */
	public final static BigInteger EXTRA_GAS_FOR_ANONYMOUS = BigInteger.valueOf(500_000L);

	/**
	 * Creates a new account by letting the faucet pay.
	 * 
	 * @param signatureAlgorithm the signature algorithm for the new account
	 * @param publicKey the public key of the new account
	 * @param balance the balance of the new account
	 * @param requestsHandler a handler called with the paid requests used for this operation. This can be useful for logging or computing costs
	 * @return the storage reference of the account
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 * @throws InvalidKeyException if the key is invalid
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws InterruptedException if the current thread gets interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 */
	StorageReference paidByFaucet(SignatureAlgorithm signatureAlgorithm, PublicKey publicKey,
			BigInteger balance, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, NodeException, InterruptedException, TimeoutException;

	/**
	 * Creates a new account by letting another account pay.
	 * 
	 * @param payer the account that pays for the creation
	 * @param keysOfPayer the keys of {@code payer}
	 * @param signatureAlgorithm the signature algorithm for the new account
	 * @param publicKey the public key of the new account
	 * @param balance the balance of the new account
	 * @param addToLedger adds the new account to the ledger of the manifest, bound to its {@code publicKey}; if an account already
	 *                    exists for {@code publicKey}, that account gets funded with {@code balance} and {@code balanceRed} coins and returned
	 * @param gasHandler a handler called with the total gas used for this operation. This can be useful for logging
	 * @param requestsHandler a handler called with the paid requests used for this operation. This can be useful for logging or computing costs
	 * @return the storage reference of the account
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 * @throws InvalidKeyException if the key is invalid
	 * @throws SignatureException if the signature of the transaction requests with {@code keysOfPayer} fails
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws NodeException if the node is not able to complete the operation
	 * @throws UnknownReferenceException if {@code payer} cannot be found in the node	
	 * @throws NoSuchAlgorithmException if the signature algorithm of {@code payer} is not available
	 */
	StorageReference paidBy(StorageReference payer, KeyPair keysOfPayer,
			SignatureAlgorithm signatureAlgorithm, PublicKey publicKey, BigInteger balance,
			boolean addToLedger,
			Consumer<BigInteger> gasHandler,
			Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException,
					NodeException, TimeoutException, InterruptedException, UnknownReferenceException, NoSuchAlgorithmException;
}