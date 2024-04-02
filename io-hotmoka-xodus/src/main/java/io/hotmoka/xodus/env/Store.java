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

package io.hotmoka.xodus.env;

import java.util.logging.Logger;

import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.ExodusException;

/**
 * A named collection of key/value pairs. This is an adapter of the Xodus {@code Store} class.
 */
public class Store {
	private final static Logger LOGGER = Logger.getLogger(Store.class.getName());
	private final jetbrains.exodus.env.Store parent;

	/**
	 * Creates a store by adapting a Xodus store.
	 * 
	 * @param parent the Xodus store to adapt
	 */
	Store(jetbrains.exodus.env.Store parent) {
		this.parent = parent;
	}

	/**
	 * Puts a key/value pair into this store and returns the result. For stores with key duplicates,
	 * it returns {@code true} if the pair didn't exist in the store. For stores without key duplicates,
	 * it returns {@code true} if the key didn't exist or the new value differs from the existing one.
	 * 
	 * @param txn the transaction during which the pair is put into this store
	 * @param key the key of the pair
	 * @param value the value of the pair
	 * @return true if and only if the specified pair was added or the value by the key was overwritten
	 * @throws ExodusException if the operation fails
	 */
	public boolean put(Transaction txn, ByteIterable key, ByteIterable value) throws ExodusException {
		try {
			boolean result = parent.put(txn.toNative(), key.toNative(), value.toNative());
			if (!result)
				// this might well be normal, if the pair was not there; it depends on the algorithm;
				// in any case, it is worth a warning
				LOGGER.warning("couldn't write key " + key + " into the Xodus store");

			return result;
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

	/**
	 * For stores without key duplicates, it returns the non-null value or {@code null}
	 * if the key doesn't exist. For stores with key duplicates, it returns the smallest
	 * non-{@code null} value associated with the key or {@code null} if no one exists.
     *
     * @param txn the transaction during which the value is read
     * @param key requested key
     * @return the non-{@code null} value if a pair with the specified key exists, otherwise {@code null}
     * @throws ExodusException if the operation fails
     */
	public ByteIterable get(Transaction txn, ByteIterable key) throws ExodusException {
		try {
			return ByteIterable.fromNative(parent.get(txn.toNative(), key.toNative()));
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

	/**
	 * Delete from this tore the pair for the given key.
	 * For stores without key duplicates, it deletes single key/value pair and returns {@code true}
	 * if a pair was deleted.
     * For stores with key duplicates, it deletes all pairs with the given key and returns
     * {@code true} if any was deleted.
     * 
	 * @param txn the transaction during which the pair is deleted from this store
	 * @param key the key of the pair to remove
	 * @return true if and only if a key/value pair was deleted.
	 * @throws ExodusException if the operation fails
	 */
	public boolean delete(Transaction txn, ByteIterable key) throws ExodusException {
		try {
			boolean result = parent.delete(txn.toNative(), key.toNative());
			if (!result)
				// this might well be normal, if the pair was not there; it depends on the algorithm;
				// in any case, it is worth a warning
				LOGGER.warning("couldn't delete key " + key + " from the Xodus store");
	
			return result;
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}
}