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

package io.hotmoka.patricia.api;

import java.util.Optional;

/**
 * A Merkle-Patricia trie.
 * 
 * @param <Key> the type of the keys of the trie
 * @param <Value> the type of the values of the trie
 * @param <T> the type of this trie
 */
public interface PatriciaTrie<Key, Value, T extends PatriciaTrie<Key, Value, T>> {

	/**
	 * Yields the value bound to the given key.
	 * 
	 * @param key the key
	 * @return the value, if any
	 * @throws TrieException if this Patricia trie is not able to complete the operation correctly
	 */
	Optional<Value> get(Key key) throws TrieException;

	/**
	 * Binds the given key to the given value. It replaces it if it was already present.
	 * This trie is not modified, but a new trie is returned instead, identical to this but for the added binding.
	 * 
	 * @param key the key
	 * @param value the value
	 * @return the resulting, modified Patricia trie
	 * @throws TrieException if this Patricia trie is not able to complete the operation correctly
	 */
	T put(Key key, Value value) throws TrieException;

	/**
	 * Yields the root of the trie, that can be used as a hash of its content.
	 * 
	 * @return the root
	 * @throws TrieException if this Patricia trie is not able to complete the operation correctly
	 */
	byte[] getRoot() throws TrieException;

	/**
	 * Yields an independent clone of this trie, but for its root, that is set to the provided value.
	 * 
	 * @param root the root to use in the cloned trie
	 * @return the resulting, cloned trie
	 * @throws TrieException if this Patricia trie is not able to complete the operation correctly
	 */
	T checkoutAt(byte[] root) throws UnknownKeyException, TrieException;
}