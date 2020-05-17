package io.hotmoka.patricia;

import java.util.NoSuchElementException;

/**
 * An abstraction of a store that persists the nodes
 * of a Patricia tree.
 */
public interface KeyValueStore {

	/**
	 * Persists an association of a key to a value in this store.
	 * It replaces it if it was already present.
	 * 
	 * @param key the key
	 * @param value the value
	 */
	void put(byte[] key, byte[] value);

	/**
	 * Gets the association of a key in this store.
	 * 
	 * @param key the key
	 * @return the value associated with the key
	 * @throws NoSuchElementException if the key is not associated in this store
	 */
	byte[] get(byte[] key) throws NoSuchElementException;

	/**
	 * Removes a key from the associations of this store, if it was there.
	 * 
	 * @param key the key to remove
	 */
	void remove(byte[] key);
}