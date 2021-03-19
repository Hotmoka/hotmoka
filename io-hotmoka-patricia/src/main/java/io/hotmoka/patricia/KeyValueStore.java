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