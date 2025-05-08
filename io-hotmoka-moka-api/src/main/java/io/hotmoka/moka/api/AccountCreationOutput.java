/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.moka.api;

import java.nio.file.Path;
import java.util.Optional;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The output of command that creates an account.
 */
@Immutable
public interface AccountCreationOutput {

	/**
	 * Yields the reference of the created account.
	 * 
	 * @return the reference of the created account; this is missing if the transaction has just been posted
	 *         rather than added, or if the transaction failed
	 */
	Optional<StorageReference> getAccount();

	/**
	 * Yields the path of the key pair file generated for the created account.
	 * 
	 * @return the path of the key pair file generated for the created account; this is
	 *         missing if the transaction has just been posted
	 *         rather than added, or if the transaction failed, or if the account has
	 *         been created with an explicit public key rather than a key pair
	 */
	Optional<Path> getFile();

	/**
	 * Yields the transaction that created the account.
	 * 
	 * @return the transaction that created the account
	 */
	TransactionReference getTransaction();

	/**
	 * Yields the gas cost of the creation transaction.
	 * 
	 * @return the gas cost of the creation transaction; this is missing if the transaction has just been posted
	 *         rather than added, or if it has been rejected
	 */
	Optional<GasCost> getGasCost();

	/**
	 * Yields the error message of the creation transaction.
	 * 
	 * @return the error message of the creation transaction; this is missing if the transaction has just been posted
	 *         rather than added, or if it was successful, or if it was rejected 
	 */
	Optional<String> getErrorMessage();
}