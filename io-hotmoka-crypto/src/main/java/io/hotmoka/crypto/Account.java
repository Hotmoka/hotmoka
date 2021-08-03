/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.crypto;

import io.hotmoka.beans.values.StorageReference;

/**
 * The information to control an account in a Hotmoka node.
 * One needs the entropy from which the key pair can be reconstructed and
 * the address of the account in the store of the node.
 */
public class Account {

	/**
	 * The entropy.
	 */
	private final byte[] entropy;

	/**
	 * The reference to the account. This is limited to have 0 as progressive, in order to reduce
	 * the information needed to represent an account as BIP39 words.
	 */
	public final StorageReference reference;

	/**
	 * Creates the information to control an account in a Hotmoka node.
	 * 
	 * @param entropy the entropy, from which the key pair can be derived
	 * @param reference the reference to the account. This is limited to have 0 as progressive,
	 *                  in order to reduce the information needed to represent an account as BIP39 words
	 */
	public Account(byte[] entropy, StorageReference reference) {
		this.entropy = entropy.clone();
		this.reference = reference;

		if (reference.progressive.signum() != 0)
			throw new IllegalArgumentException("accounts are limited to have 0 as progressive index");
	}

	/**
	 * Yields the entropy of this account, from which its key pair can be reconstructed.
	 * 
	 * @return the entropy
	 */
	public byte[] getEntropy() {
		return entropy.clone();
	}
}