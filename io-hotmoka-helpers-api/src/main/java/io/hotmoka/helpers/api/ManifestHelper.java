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

import java.util.concurrent.TimeoutException;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.values.StorageReference;

/**
 * An object that helps with the access to the manifest of a node.
 */
@ThreadSafe
public interface ManifestHelper {

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
	 * Yields the chain id of the node. This might throw exceptions since
	 * it performs an actual query on the node, being the chain id potentially
	 * variable when consensus changes.
	 * 
	 * @return the chain id
	 * @throws ClosedNodeException if the node is already closed
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	String getChainId() throws ClosedNodeException, TimeoutException, InterruptedException;

	@Override
	String toString();
}