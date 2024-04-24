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

import io.hotmoka.marshalling.api.Marshallable;

/**
 * A Merkle-Patricia trie.
 * 
 * @param <Key> the type of the keys of the trie
 * @param <Value> the type of the values of the trie
 */
public interface PatriciaTrie<Key, Value extends Marshallable> {

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
	 * 
	 * @param key the key
	 * @param value the value
	 * @throws TrieException if this Patricia trie is not able to complete the operation correctly
	 */
	void put(Key key, Value value) throws TrieException;

	/**
	 * Yields the root of the trie, that can be used as a hash of its content.
	 * 
	 * @return the root
	 * @throws TrieException if this Patricia trie is not able to complete the operation correctly
	 */
	byte[] getRoot() throws TrieException;

	/**
	 * Garbage-collects all keys that have been updated during the commit with the given number.
	 * 
	 * @param commitNumber the number of the commit to garbage collect
	 * @throws TrieException if this Patricia trie is not able to complete the operation correctly
	 */
	void garbageCollect(long commitNumber) throws TrieException;
}