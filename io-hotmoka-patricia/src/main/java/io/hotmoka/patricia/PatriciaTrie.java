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

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.Marshallable.Unmarshaller;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.patricia.internal.PatriciaTrieImpl;

/**
 * A Merkle-Patricia trie.
 */
public interface PatriciaTrie<Key, Value extends Marshallable> {

	/**
	 * Yields the value bound to the given key.
	 * 
	 * @param key the key
	 * @return the value, if any
	 */
	Optional<Value> get(Key key);

	/**
	 * Binds the given key to the given value. It replaces it
	 * if already present.
	 * 
	 * @param key the key
	 * @param value the value
	 */
	void put(Key key, Value value);

	/**
	 * Yields the root of the trie, that can be used as a hash of its content.
	 * 
	 * @return the root
	 */
	byte[] getRoot();

	/**
	 * Yields the Merkle-Patricia trie supported by the underlying store,
	 * using the given hashing algorithm to hash nodes, keys and the values.
	 * 
	 * @param store the store used to store a mapping from nodes' hashes to their content
	 * @param hashingForKeys the hashing algorithm for the keys
	 * @param hashingForNodes the hashing algorithm for the nodes of the trie
	 * @param valueUnmarshaller a function able to unmarshall a value from its byte representation
	 * @param garbageCollected true if and only if unused nodes must be garbage collected; in general,
	 *                         this can be true if previous configurations of the trie needn't be
	 *                         rechecked out in the future
	 * @return the trie
	 */
	static <Key, Value extends Marshallable> PatriciaTrie<Key, Value> of
			(KeyValueStore store,
			HashingAlgorithm<? super Key> hashingForKeys, HashingAlgorithm<? super Node> hashingForNodes,
			Unmarshaller<? extends Value> valueUnmarshaller,
			boolean garbageCollected) {

		return new PatriciaTrieImpl<>(store, hashingForKeys, hashingForNodes, valueUnmarshaller, garbageCollected);
	}
}