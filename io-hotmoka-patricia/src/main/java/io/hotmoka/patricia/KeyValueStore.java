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

import java.util.NoSuchElementException;

/**
 * An abstraction of a store that persists the nodes
 * of a Patricia tree.
 */
public interface KeyValueStore {

	/**
	 * Yields the hash of the root of the Patricia trie
	 * that this store supports.
	 * 
	 * @return the hash of the root; this might be {@code null}
	 *         if this store supports the empty Patricia trie
	 */
	byte[] getRoot();

	/**
	 * Sets the hash of the root of the Patricia trie
	 * that this store supports.
	 * 
	 * @param root the hash of the root of the trie
	 */
	void setRoot(byte[] root);

	/**
	 * Persists an association of a key to a value in this store.
	 * It replaces it if it was already present.
	 * 
	 * @param key the key
	 * @param value the value
	 */
	void put(byte[] key, byte[] value);

	/**
	 * Deletes the association for the given key, that must exist in store.
	 * 
	 * @param key the key
	 */
	void remove(byte[] key);

	/**
	 * Gets the association of a key in this store.
	 * 
	 * @param key the key
	 * @return the value associated with the key
	 * @throws NoSuchElementException if the key is not associated in this store
	 */
	byte[] get(byte[] key) throws NoSuchElementException;
}