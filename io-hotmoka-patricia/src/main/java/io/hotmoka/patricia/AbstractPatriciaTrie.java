/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.patricia;

import java.util.Optional;

import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.patricia.api.KeyValueStore;
import io.hotmoka.patricia.api.TrieException;
import io.hotmoka.patricia.internal.AbstractPatriciaTrieImpl;

/**
 * Abstract implementation of a Patricia trie.
 *
 * @param <Key> the type of the keys of the trie
 * @param <Value> the type of the values of the trie
 * @param <T> the type of this trie
 */
public abstract class AbstractPatriciaTrie<Key, Value, T extends AbstractPatriciaTrie<Key, Value, T>> extends AbstractPatriciaTrieImpl<Key, Value, T> {

	/**
	 * Creates an empty Merkle-Patricia trie supported by the given underlying store,
	 * using the given hashing algorithms to hash nodes and values.
	 * 
	 * @param store the store used to store the nodes of the tree, as a mapping from nodes' hashes
	 *              to the marshalled representation of the nodes
	 * @param root the root of the trie; pass it empty to create an empty trie
	 * @param hasherForKeys the hasher for the keys
	 * @param hashingForNodes the hashing algorithm for the nodes of the trie
	 * @param valueToBytes a function that marshals values into their byte representation
	 * @param bytesToValue a function that unmarshals bytes into the represented value
	 * @throws TrieException if the creation cannot be completed correctly
	 */
	protected AbstractPatriciaTrie(KeyValueStore store, Optional<byte[]> root,
			Hasher<? super Key> hasherForKeys, HashingAlgorithm hashingForNodes,
			ToBytes<? super Value> valueToBytes, FromBytes<? extends Value> bytesToValue) throws TrieException {

		super(store, root, hasherForKeys, hashingForNodes, valueToBytes, bytesToValue);
	}

	/**
	 * Creates a Merkle-Patricia trie from the given trie, checked out at the given root.
	 * 
	 * @param cloned the trie from which the result will be derived; it is assumed that this
	 *               trie has been derived by a sequence of put operations passing through
	 *               the given root, that has not been garbage-collected yet
	 * @param root the root used to check out the trie
	 * @throws TrieException if the creation cannot be completed correctly
	 */
	protected AbstractPatriciaTrie(T cloned, byte[] root) throws TrieException {
		super(cloned, root);
	}
}