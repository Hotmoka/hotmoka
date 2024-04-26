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

import io.hotmoka.patricia.api.UnknownKeyException;

/**
 * An abstraction of a store that persists the nodes of a Patricia tree.
 */
public interface KeyValueStore {

	/**
	 * Persists an association of a key to a value in this store.
	 * It replaces it if it was already present.
	 * 
	 * @param key the key; this might be missing in this store, in which case nothing happens
	 * @param value the value
	 * @throws KeyValueStoreException if this key/value store is not able to complete the operation
	 */
	void put(byte[] key, byte[] value) throws KeyValueStoreException;

	/**
	 * Deletes the association for the given key, that must exist in store.
	 * 
	 * @param key the key
	 * @throws UnknownKeyException if {@code key} is not present in this key/value store
	 * @throws KeyValueStoreException if this key/value store is not able to complete the operation
	 */
	void remove(byte[] key) throws UnknownKeyException, KeyValueStoreException;

	/**
	 * Gets the association of a key in this store.
	 * 
	 * @param key the key
	 * @return the value associated with the key
	 * @throws UnknownKeyException if {@code key} is not present in this key/value store
	 * @throws KeyValueStoreException if this key/value store is not able to complete the operation
	 */
	byte[] get(byte[] key) throws UnknownKeyException, KeyValueStoreException;
}