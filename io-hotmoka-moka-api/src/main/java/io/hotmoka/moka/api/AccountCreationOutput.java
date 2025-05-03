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

import java.math.BigInteger;
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
	 * Yields the transaction that created the account.
	 * 
	 * @return the transaction that created the account
	 */
	TransactionReference getTransaction();

	/**
	 * Yields the reference of the created account.
	 * 
	 * @return the reference of the created account
	 */
	StorageReference getAccount();

	/**
	 * Yields the path of the key pair file generated for the created account.
	 * 
	 * @return the path of the key pair file generated for the created account
	 */
	Optional<Path> getFile();

	/**
	 * Yields the amount of gas consumed for the CPU cost for creating the account.
	 * 
	 * @return the amount of gas consumed for the CPU cost for creating the account
	 */
	BigInteger getGasConsumedForCPU();

	/**
	 * Yields the amount of gas consumed for the RAM cost for creating the account.
	 * 
	 * @return the amount of gas consumed for the RAM cost for creating the account
	 */
	BigInteger getGasConsumedForRAM();

	/**
	 * Yields the amount of gas consumed for the storage cost for creating the account.
	 * 
	 * @return the amount of gas consumed for the storage cost for creating the account
	 */
	BigInteger getGasConsumedForStorage();
}