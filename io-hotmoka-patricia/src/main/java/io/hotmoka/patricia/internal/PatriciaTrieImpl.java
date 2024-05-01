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

package io.hotmoka.patricia.internal;

import java.util.Optional;

import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.patricia.FromBytes;
import io.hotmoka.patricia.ToBytes;
import io.hotmoka.patricia.api.KeyValueStore;

/**
 * Implementation of a Merkle-Patricia trie.
 *
 * @param <Key> the type of the keys of the trie
 * @param <Value> the type of the values of the trie
 */
public class PatriciaTrieImpl<Key, Value> extends AbstractPatriciaTrieImpl<Key, Value, PatriciaTrieImpl<Key, Value>> {

	/**
	 * Creates a new Merkle-Patricia trie supported by the given underlying store,
	 * using the given hashing algorithms to hash nodes and values.
	 * 
	 * @param store the store used to store the nodes of the tree, as a mapping from nodes' hashes
	 *              to the marshalled representation of the nodes
	 * @param root the root of the trie; pass it empty to create an empty trie
	 * @param hasherForKeys the hasher for the keys
	 * @param hashingForNodes the hashing algorithm for the nodes of the trie
	 * @param valueToBytes a function that marshals values into their byte representation
	 * @param bytesToValue a function that unmarshals bytes into the represented value
	 * @param numberOfCommits the current number of commits already executed on the store; this trie
	 *                        will record which data can be garbage collected (eventually)
	 *                        because they become unreachable as result of the store updates
	 *                        performed during commit {@code numerOfCommits}; this value could
	 *                        be -1L if the trie is only used for reading, so that there is no need
	 *                        to keep track of keys that can be garbage-collected
	 */
	public PatriciaTrieImpl(KeyValueStore store, Optional<byte[]> root,
			Hasher<? super Key> hasherForKeys, HashingAlgorithm hashingForNodes,
			ToBytes<? super Value> valueToBytes, FromBytes<? extends Value> bytesToValue, long numberOfCommits) {

		super(store, root, hasherForKeys, hashingForNodes, valueToBytes, bytesToValue, numberOfCommits);
	}

	/**
	 * Clones the given trie, but for its root, that is set to the provided value.
	 * 
	 * @param cloned the trie to clone
	 * @param root the root to use in the cloned trie
	 */
	private PatriciaTrieImpl(PatriciaTrieImpl<Key, Value> cloned, byte[] root) {
		super(cloned, root);
	}

	/**
	 * Clones the given trie, but for its supporting store, that is set to the provided value.
	 * 
	 * @param cloned the trie to clone
	 * @param store the store to use in the cloned trie
	 */
	private PatriciaTrieImpl(PatriciaTrieImpl<Key, Value> cloned, KeyValueStore store) {
		super(cloned, store);
	}

	@Override
	public PatriciaTrieImpl<Key, Value> checkoutAt(byte[] root) {
		return new PatriciaTrieImpl<>(this, root);
	}
}