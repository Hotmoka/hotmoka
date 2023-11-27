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
import java.util.function.Consumer;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.api.SignatureAlgorithm;

/**
 * An object that helps with the creation of new accounts.
 */
@ThreadSafe
public interface AccountCreationHelper {

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
	 * @param balanceRed the red balance of the new account
	 * @param requestsHandler a handler called with the paid requests used for this operation. This can be useful for logging or computing costs
	 * @return the storage reference of the account
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 * @throws InvalidKeyException if the key is invalid
	 * @throws SignatureException if some signature failed
	 */
	StorageReference paidByFaucet(SignatureAlgorithm signatureAlgorithm, PublicKey publicKey,
			BigInteger balance, BigInteger balanceRed, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException;

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
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 * @throws InvalidKeyException if the key is invalid
	 * @throws SignatureException if some signature failed
	 * @throws NoSuchAlgorithmException if the payer uses an unknown signature algorithm
	 * @throws ClassNotFoundException if the class of the payer is unknown
	 */
	StorageReference paidBy(StorageReference payer, KeyPair keysOfPayer,
			SignatureAlgorithm signatureAlgorithm, PublicKey publicKey, BigInteger balance, BigInteger balanceRed,
			boolean addToLedger,
			Consumer<BigInteger> gasHandler,
			Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException;

	/**
	 * Creates a new Tendermint validator by letting the faucet pay.
	 * 
	 * @param publicKey the public key of the new validator
	 * @param balance the balance of the new validator
	 * @param balanceRed the red balance of the new validator
	 * @param requestsHandler a handler called with the paid requests used for this operation. This can be useful for logging or computing costs
	 * @return the storage reference of the validator
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 * @throws InvalidKeyException if the key is invalid
	 * @throws SignatureException if some signature failed
	 * @throws NoSuchAlgorithmException if the faucet uses an unknown signature algorithm
	 */
	StorageReference tendermintValidatorPaidByFaucet(PublicKey publicKey,
			BigInteger balance, BigInteger balanceRed, Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException;

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
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 * @throws InvalidKeyException if the key is invalid
	 * @throws SignatureException if some signature failed
	 * @throws NoSuchAlgorithmException if the payer uses an unknown signature algorithm
	 * @throws ClassNotFoundException if the class of the payer is unknown
	 */
	StorageReference tendermintValidatorPaidBy(StorageReference payer, KeyPair keysOfPayer, PublicKey publicKey, BigInteger balance, BigInteger balanceRed,
			Consumer<BigInteger> gasHandler,
			Consumer<TransactionRequest<?>[]> requestsHandler)
			throws TransactionRejectedException, TransactionException, CodeExecutionException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, ClassNotFoundException;
}