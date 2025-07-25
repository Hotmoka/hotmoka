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

package io.hotmoka.moka.internal;

import java.util.Optional;

import io.hotmoka.crypto.Base58;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The specification of an account, given as either its storage reference or as
 * the a base58-encoded key that can be looked up inside the accounts ledger of a node.
 */
public class StorageReferenceOrBase58Key {
	private final StorageReference reference;
	private final String publicKeyBase58;

	/**
	 * Builds the specification of an account, that is either its storage reference
	 * or the base58-encoded public key of the account, that can be looked up in the
	 * accounts ledger of the node.
	 * 
	 * @param s the string
	 * @throws IllegalArgumentException if the conversion is impossible
	 */
	public StorageReferenceOrBase58Key(String s) throws IllegalArgumentException {
		StorageReference reference = null;
		String publicKeyBase58 = null;

		try {
			// we first try to transform it into a storage reference
			reference = StorageValues.reference(s);
		}
		catch (IllegalArgumentException e) {
			// otherwise we try to transform it into a Base58-encoded string
			publicKeyBase58 = Base58.requireBase58(s, IllegalArgumentException::new);
		}

		this.reference = reference;
		this.publicKeyBase58 = publicKeyBase58;
	}

	/**
	 * Yields the account as a storage reference.
	 * 
	 * @return the account as a storage reference, if any
	 */
	public Optional<StorageReference> asReference() {
		return Optional.ofNullable(reference);
	}

	/**
	 * Yields the account as a base58-encoded key.
	 * 
	 * @return the account as a base58-encoded key, if any
	 */
	public Optional<String> asBase58Key() {
		return Optional.ofNullable(publicKeyBase58);
	}

	@Override
	public String toString() {
		return reference != null ? reference.toString() : publicKeyBase58;
	}
}