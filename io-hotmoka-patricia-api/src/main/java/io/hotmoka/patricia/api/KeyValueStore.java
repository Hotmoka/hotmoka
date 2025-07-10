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

/**
 * An abstraction of a key/value persistent store of a Patricia tree.
 */
public interface KeyValueStore {

	/**
	 * Persists a key/value association in this store.
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
	 * @throws UnknownKeyException if {@code key} is not present in this key/value store
	 */
	void remove(byte[] key) throws UnknownKeyException;

	/**
	 * Gets the association of a key in this store.
	 * 
	 * @param key the key
	 * @return the value associated with the key
	 * @throws UnknownKeyException if {@code key} is not present in this key/value store
	 */
	byte[] get(byte[] key) throws UnknownKeyException;
}