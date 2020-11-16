package io.takamaka.code.util;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.takamaka.code.lang.View;
import io.takamaka.code.util.internal.StorageMapView;

/**
 * A map from storage keys to (possibly {@code null}) storage values,
 * that can be kept in storage. By iterating on this object, one gets
 * the key/value pairs of the map, in increasing key order.
 * This interface has only access methods. Its sub-interface
 * {@link #io.takamaka.code.util.ModifiableStorageMap} includes
 * modification methods as well.
 * 
 * @param <K> the type of the keys
 * @param <V> the type of the values
 */

public interface StorageMap<K,V> extends Iterable<StorageMap.Entry<K,V>> {

	/**
	 * A key/value pair.
	 *
	 * @param <K> the type of the keys
	 * @param <V> the type of the values
	 */
	interface Entry<K,V> {
		K getKey();
		V getValue();
	}

	/**
	 * Yields an exported view of the given parent map. All changes in the parent map
	 * are reflected in the view. The parent map cannot be modified
	 * through the view, since the latter misses any modification method.
	 * 
	 * @param <K> the type of the keys of the view
	 * @param <V> the type of the values of the view
	 * @param parent the parent map
	 * @return the resulting view
	 */
	static <K,V> StorageMap<K,V> viewOf(StorageMap<K,V> parent) {
		return new StorageMapView<>(parent);
	}

	/**
	 * Returns the number of key-value pairs in this symbol table.
	 * 
	 * @return the number of key-value pairs in this symbol table
	 */
	@View int size();

	/**
	 * Determines if this symbol table is empty.
	 * 
	 * @return {@code true} if and only if this symbol table is empty
	 */
	@View boolean isEmpty();

	/**
	 * Yields the value associated with the given key, if any.
	 * 
	 * @param key the key
	 * @return the value associated with the given key if the key is in the symbol table
	 *         and {@code null} if the key is not in the symbol table
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	@View V get(Object key);

	/**
	 * Returns the value associated with the given key.
	 * 
	 * @param key the key
	 * @return the value associated with the given key if the key is in the symbol table.
	 *         Yields {@code _default} if the key is not in the symbol table
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	@View V getOrDefault(Object key, V _default);

	/**
	 * Yields the value associated with the given key.
	 * 
	 * @param key the key
	 * @return the value associated with the given key if the key is in the symbol table.
	 *         Yields {@code _default.get()} if the key is not in the symbol table
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	V getOrDefault(Object key, Supplier<V> _default);

	/**
	 * Determines if this symbol table contain the given key.
	 * 
	 * @param key the key
	 * @return {@code true} if and only if this symbol table contains {@code key}
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	@View boolean contains(Object key);

	/**
	 * Yields the smallest key in the symbol table.
	 * 
	 * @return the smallest key in the symbol table
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	@View K min();

	/**
	 * Yields the largest key in the symbol table.
	 * 
	 * @return the largest key in the symbol table
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	@View K max();

	/**
	 * Yields the largest key in the symbol table less than or equal to {@code key}.
	 * 
	 * @param key the key
	 * @return the largest key in the symbol table less than or equal to {@code key}
	 * @throws NoSuchElementException if there is no such key
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	@View K floorKey(K key);

	/**
	 * Yields the smallest key in the symbol table greater than or equal to {@code key}.
	 * 
	 * @param key the key
	 * @return the smallest key in the symbol table greater than or equal to {@code key}
	 * @throws NoSuchElementException if there is no such key
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	@View K ceilingKey(K key);

	/**
	 * Yields the key in the symbol table whose rank is {@code k}.
	 * This is the (k+1)st smallest key in the symbol table. 
	 *
	 * @param  k the rank
	 * @return the key in the symbol table of rank {@code k}
	 * @throws IllegalArgumentException unless {@code k} is between 0 and {@code size()-1}
	 */
	@View K select(int k);

	/**
	 * Yields the number of keys in the symbol table strictly less than {@code key}.
	 * 
	 * @param key the key
	 * @return the number of keys in the symbol table strictly less than {@code key}
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	@View int rank(K key);

	/**
	 * Yields an ordered stream of the entries (key/value) in this map, in
	 * increasing order of keys.
	 * 
	 * @return the stream
	 */
	Stream<Entry<K,V>> stream();

	/**
	 * Yields the keys of this map, in increasing order.
	 * 
	 * @return the keys
	 */
	List<K> keyList();

	/**
	 * Yields the ordered stream of the keys of this map, in increasing order.
	 * 
	 * @return the stream
	 */
	Stream<K> keys();
}