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

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;

/**
 * An object that helps with the access to the manifest of a node.
 */
public interface ManifestHelper {

	/**
	 * Yields the reference to the transaction that installed
	 * the Takamaka code in the node.
	 * 
	 * @return the reference to the transaction that installed the Takamaka code
	 */
	TransactionReference getTakamakaCode();

	/**
	 * Yields the accounts ledger of the node.
	 * 
	 * @return the reference to the accounts ledger
	 */
	StorageReference getAccountsLedger();

	/**
	 * Yields the initial validators of the node.
	 * 
	 * @return the reference to the initial validators
	 */
	StorageReference getInitialValidators();

	/**
	 * Yields the validators of the node.
	 * 
	 * @return the reference to the validators
	 */
	StorageReference getValidators();

	/**
	 * Yields the manifest of the node.
	 * 
	 * @return the reference to the manifest
	 */
	StorageReference getManifest();

	/**
	 * Yields the versions of the node.
	 * 
	 * @return the reference to the versions
	 */
	StorageReference getVersions();

	/**
	 * Yields the gamete of the node.
	 * 
	 * @return the reference to the gamete
	 */
	StorageReference getGamete();

	/**
	 * Yields the gas station of the node.
	 * 
	 * @return the reference to the gas station
	 */
	StorageReference getGasStation();

	/**
	 * Yields the chain id of the node.
	 * 
	 * @return the chain id
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 */
	String getChainId() throws TransactionRejectedException, TransactionException, CodeExecutionException;

	@Override
	String toString();
}