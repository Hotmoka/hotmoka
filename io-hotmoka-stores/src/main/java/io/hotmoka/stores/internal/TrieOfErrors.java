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

package io.hotmoka.stores.internal;

import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import io.hotmoka.crypto.HashingAlgorithms;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.patricia.AbstractPatriciaTrie;
import io.hotmoka.patricia.api.KeyValueStore;
import io.hotmoka.patricia.api.TrieException;

/**
 * A map from transaction requests into their error, backed by a Merkle-Patricia trie.
 */
public class TrieOfErrors extends AbstractPatriciaTrie<TransactionReference, String, TrieOfErrors> {

	/**
	 * Builds a Merkle-Patricia trie that maps transaction requests into their errors.
	 * 
	 * @param store the supporting key/value store
	 * @param txn the transaction where updates are reported
	 * @param root the root of the trie to check out; use empty to create the empty trie
	 */
	public TrieOfErrors(KeyValueStore store, Optional<byte[]> root) throws TrieException {
		super(store, root, HashingAlgorithms.identity32().getHasher(TransactionReference::getHash),
			sha256(), String::getBytes, String::new, -1L);
	}

	private TrieOfErrors(TrieOfErrors cloned, byte[] root) {
		super(cloned, root);
	}

	/**
	 * Clones the given trie, but for its supporting store, that is set to the provided value.
	 * 
	 * @param cloned the trie to clone
	 * @param store the store to use in the cloned trie
	 */
	private TrieOfErrors(TrieOfErrors cloned, KeyValueStore store) {
		super(cloned, store);
	}

	@Override
	public TrieOfErrors checkoutAt(byte[] root) {
		return new TrieOfErrors(this, root);
	}

	private static HashingAlgorithm sha256() throws TrieException {
		try {
			return HashingAlgorithms.sha256();
		}
		catch (NoSuchAlgorithmException e) {
			throw new TrieException(e);
		}
	}
}