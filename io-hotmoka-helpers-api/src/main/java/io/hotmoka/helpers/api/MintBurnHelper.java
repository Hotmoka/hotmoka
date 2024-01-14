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
import java.security.SignatureException;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.crypto.api.SignatureAlgorithm;

/**
 * A helper for minting and burning coins of an account in the accounts ledger
 * of the node. Only the gamete can do that and only if the node allows mint and burn from the gamete.
 */
public interface MintBurnHelper {

	/**
	 * Mints fresh new coins for an account in the accounts ledger. If the account does not exist yet there, it is created.
	 * 
	 * @param keysOfGamete the keys of the gamete
	 * @param signatureAlgorithm the signature to use for the account
	 * @param publicKey the Base64-encoded public key of the account
	 * @param amount the amount of coins to mint
	 * @return the account, old or brand new
	 * @throws InvalidKeyException if the key is invalid
	 * @throws SignatureException if some signature failed
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 */
	StorageReference mint(KeyPair keysOfGamete, SignatureAlgorithm signatureAlgorithm, String publicKey, BigInteger amount) throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException;

	/**
	 * Burns coins from an account in the accounts ledger. If the account does not exist yet there, it throws a transaction exception.
	 * 
	 * @param keysOfGamete the keys of the gamete
	 * @param signatureAlgorithm the signature to use for the account
	 * @param publicKey the Base64-encoded public key of the account
	 * @param amount the amount of coins to burn
	 * @return the account whose coins have been burnt
	 * @throws InvalidKeyException if the key is invalid
	 * @throws SignatureException if some signature failed
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 */
	StorageReference burn(KeyPair keysOfGamete, SignatureAlgorithm signatureAlgorithm, String publicKey, BigInteger amount) throws InvalidKeyException, SignatureException, TransactionRejectedException, TransactionException, CodeExecutionException;
}