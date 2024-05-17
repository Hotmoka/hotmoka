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

import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.values.StorageReference;

/**
 * An object that helps with the access to the manifest of a node.
 */
public interface ManifestHelper {

	/**
	 * Yields the accounts ledger of the node.
	 * 
	 * @return the reference to the accounts ledger
	 */
	StorageReference getAccountsLedger() throws NodeException, TimeoutException, InterruptedException;

	/**
	 * Yields the initial validators of the node.
	 * 
	 * @return the reference to the initial validators
	 */
	StorageReference getInitialValidators() throws NodeException, TimeoutException, InterruptedException;

	/**
	 * Yields the validators of the node.
	 * 
	 * @return the reference to the validators
	 */
	StorageReference getValidators() throws NodeException, TimeoutException, InterruptedException;

	/**
	 * Yields the manifest of the node.
	 * 
	 * @return the reference to the manifest
	 */
	StorageReference getManifest() throws NodeException, TimeoutException, InterruptedException;

	/**
	 * Yields the versions of the node.
	 * 
	 * @return the reference to the versions
	 */
	StorageReference getVersions() throws NodeException, TimeoutException, InterruptedException;

	/**
	 * Yields the gamete of the node.
	 * 
	 * @return the reference to the gamete
	 */
	StorageReference getGamete() throws NodeException, TimeoutException, InterruptedException;

	/**
	 * Yields the gas station of the node.
	 * 
	 * @return the reference to the gas station
	 */
	StorageReference getGasStation() throws NodeException, TimeoutException, InterruptedException;

	/**
	 * Yields the chain id of the node.
	 * 
	 * @return the chain id
	 */
	String getChainId() throws NodeException, TimeoutException, InterruptedException;

	@Override
	String toString();
}