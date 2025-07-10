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

import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.patricia.AbstractPatriciaTrie;
import io.hotmoka.patricia.FromBytes;
import io.hotmoka.patricia.ToBytes;
import io.hotmoka.patricia.api.KeyValueStore;
import io.hotmoka.patricia.api.UnknownKeyException;

/**
 * Implementation of a Merkle-Patricia trie.
 *
 * @param <Key> the type of the keys of the trie
 * @param <Value> the type of the values of the trie
 */
public class PatriciaTrieImpl<Key, Value> extends AbstractPatriciaTrie<Key, Value, PatriciaTrieImpl<Key, Value>> {

	/**
	 * Creates a new Merkle-Patricia trie supported by the given underlying store,
	 * using the given hashing algorithms to hash nodes and values.
	 * 
	 * @param store the store used to store the nodes of the trie, as a mapping from nodes' hashes
	 *              to the marshalled representation of the nodes
	 * @param root the root of the trie
	 * @param hasherForKeys the hasher for the keys
	 * @param hashingForNodes the hashing algorithm for the nodes of the trie
	 * @param hashOfEmpty the hash of the empty trie
	 * @param valueToBytes a function that marshals values into their byte representation
	 * @param bytesToValue a function that unmarshals bytes into the represented value
	 * @throws UnknownKeyException if {@code root} is unknown in the store of the trie
	 */
	public PatriciaTrieImpl(KeyValueStore store, byte[] root,
			Hasher<? super Key> hasherForKeys, HashingAlgorithm hashingForNodes, byte[] hashOfEmpty,
			ToBytes<? super Value> valueToBytes, FromBytes<? extends Value> bytesToValue) throws UnknownKeyException {

		super(store, root, hasherForKeys, hashingForNodes, hashOfEmpty, valueToBytes, bytesToValue);
	}

	/**
	 * Clones the given trie, but for its root, that is set to the provided value.
	 * 
	 * @param cloned the trie to clone
	 * @param root the root to use in the cloned trie
	 * @throws UnknownKeyException if {@code root} is unknown in the store of the trie
	 */
	private PatriciaTrieImpl(PatriciaTrieImpl<Key, Value> cloned, byte[] root) throws UnknownKeyException {
		super(cloned, root);
	}

	@Override
	public PatriciaTrieImpl<Key, Value> checkoutAt(byte[] root) throws UnknownKeyException {
		return new PatriciaTrieImpl<>(this, root);
	}
}