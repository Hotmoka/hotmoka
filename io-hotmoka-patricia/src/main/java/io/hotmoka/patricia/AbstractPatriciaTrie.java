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

package io.hotmoka.patricia;

import java.util.Optional;

import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.patricia.api.FromBytes;
import io.hotmoka.patricia.api.KeyValueStore;
import io.hotmoka.patricia.api.ToBytes;
import io.hotmoka.patricia.internal.PatriciaTrieImpl;

/**
 * Abstract implementation of a Patricia trie.
 *
 * @param <Key> the type of the keys of the trie
 * @param <Value> the type of the values of the trie
 */
public abstract class AbstractPatriciaTrie<Key, Value> extends PatriciaTrieImpl<Key, Value> {

	/**
	 * Creates a new Merkle-Patricia trie supported by the underlying store,
	 * using the given hashing algorithm to hash nodes and values.
	 * 
	 * @param store the store used to store a mapping from nodes' hashes to the marshalled
	 *              representation of the nodes
	 * @param root the root of the trie; use empty to create an empty trie
	 * @param hasherForKeys the hasher for the keys
	 * @param hashingForNodes the hashing algorithm for the nodes of the trie
	 * @param valueUnmarshaller a function able to unmarshall a value from its byte representation
	 * @param valueUnmarshallingContextSupplier the supplier of the unmarshalling context for the values
	 * @param numberOfCommits the current number of commits already executed on the store; this trie
	 *                        will record which data can be garbage collected (eventually)
	 *                        because they become unreachable as result of the store updates
	 *                        performed during commit {@code numerOfCommits}; this value could
	 *                        be -1L if the trie is only used or reading, so that there is no need
	 *                        to keep track of keys that can be garbage-collected
	 */
	protected AbstractPatriciaTrie(KeyValueStore store, Optional<byte[]> root,
			Hasher<? super Key> hasherForKeys, HashingAlgorithm hashingForNodes,
			ToBytes<? super Value> bytesFromValue,
			FromBytes<? extends Value> valueFromBytes,
			long numberOfCommits) {

		super(store, root, hasherForKeys, hashingForNodes, bytesFromValue, valueFromBytes, numberOfCommits);
	}
}