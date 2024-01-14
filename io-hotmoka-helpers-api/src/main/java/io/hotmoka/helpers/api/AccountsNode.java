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

import java.security.PrivateKey;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.api.Node;

/**
 * A node that provides access to a previously installed set of accounts.
 */
@ThreadSafe
public interface AccountsNode extends Node {

	/**
	 * Yields the accounts.
	 * 
	 * @return the references to the accounts. This is an instance of {@code io.takamaka.code.lang.Accounts}
	 */
	Stream<StorageReference> accounts();

	/**
	 * Yields the private keys for controlling the accounts.
	 * 
	 * @return the private keys, in the same order as {@link #accounts()}
	 */
	Stream<PrivateKey> privateKeys();

	/**
	 * Yields the container of the accounts that have been created.
	 * 
	 * @return the container. This is an instance of {@code io.takamaka.code.lang.Accounts}
	 */
	StorageReference container();

	/**
	 * Yields the {@code i}th account.
	 * 
	 * @param i the account number
	 * @return the reference to the account, in the store of the node. This is an {@code io.takamaka.code.lang.ExternallyOwnedAccount}
	 * @throws NoSuchElementException if the {@code i}th account does not exist
	 */
	StorageReference account(int i) throws NoSuchElementException;

	/**
	 * Yields the private key for controlling the {@code i}th account.
	 * 
	 * @param i the account number
	 * @return its private key
	 * @throws NoSuchElementException if the {@code i}th account does not exist
	 */
	PrivateKey privateKey(int i) throws NoSuchElementException;
}