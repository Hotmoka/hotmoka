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

import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The specification of a transaction payer, given as either a storage reference or the faucet.
 */
public class StorageReferenceOrFaucet {
	private final StorageReference reference;

	/**
	 * Builds a payer from a string, that is either "faucet" or a storage reference.
	 * 
	 * @param s the string
	 * @throws IllegalArgumentException if the conversion is impossible
	 */
	public StorageReferenceOrFaucet(String s) throws IllegalArgumentException {
		if ("faucet".equals(s))
			this.reference = null;
		else
			this.reference = StorageValues.reference(s);
	}

	/**
	 * Yields this payer as a storage reference.
	 * 
	 * @return the payer as a storage reference, if any
	 */
	public Optional<StorageReference> asReference() {
		return Optional.ofNullable(reference);
	}

	/**
	 * Determines if this payer is the faucet.
	 * 
	 * @return true if and only if this payer is the faucet
	 */
	public boolean isFaucet() {
		return reference == null;
	}

	@Override
	public String toString() {
		return reference == null ? "the faucet" : reference.toString();
	}
}