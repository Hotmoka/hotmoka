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

import java.io.IOException;
import java.io.InputStream;

import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.marshalling.api.Marshallable;
import io.hotmoka.marshalling.api.Unmarshaller;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.patricia.api.KeyValueStore;
import io.hotmoka.patricia.api.PatriciaTrie;
import io.hotmoka.patricia.internal.PatriciaTrieImpl;

/**
 * Provider of Merkle-Patricia tries.
 */
public final class PatriciaTries {

	private PatriciaTries() {}

	/**
	 * Yields a Merkle-Patricia trie supported by the underlying store,
	 * using the given hashing algorithm to hash nodes, keys and the values.
	 * 
	 * @param <Key> the type of the keys of the trie
	 * @param <Value> the type of the values of the trie
	 * @param store the store used to store a mapping from nodes' hashes to their content
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
	 * @return the trie
	 */
	public static <Key, Value extends Marshallable> PatriciaTrie<Key, Value> of
			(KeyValueStore store,
			Hasher<? super Key> hasherForKeys, HashingAlgorithm hashingForNodes,
			Unmarshaller<? extends Value> valueUnmarshaller,
			UnmarshallingContextSupplier valueUnmarshallingContextSupplier, long numberOfCommits) {

		return new PatriciaTrieImpl<>(store, hasherForKeys, hashingForNodes, valueUnmarshaller, valueUnmarshallingContextSupplier, numberOfCommits);
	}

	/**
	 * A function that supplies an unmarshalling context.
	 */
	public interface UnmarshallingContextSupplier {

		/**
		 * Yields the unmarshalling context.
		 * 
		 * @param is the input stream of the context
		 * @return the unmarshalling context
		 * @throws IOException if the context cannot be created
		 */
		UnmarshallingContext get(InputStream is) throws IOException;
	}
}