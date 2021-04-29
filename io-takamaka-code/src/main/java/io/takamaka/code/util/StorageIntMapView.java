package io.takamaka.code.util;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.takamaka.code.lang.View;

/**
 * A map from integer keys to (possibly {@code null}) storage values,
 * that can be kept in storage. By iterating on this object, one gets
 * the key/value pairs of the map, in increasing key order. This interface includes
 * read methods only.
 * 
 * @param <V> the type of the values
 */
public interface StorageIntMapView<V> extends Iterable<StorageIntMapView.Entry<V>> {

	/**
	 * A key/value pair.
	 *
	 * @param <V> the type of the values
	 */
	interface Entry<V> {
		int getKey();
		V getValue();
	}

	/**
	 * Returns the number of key-value pairs in this symbol table.
	 * 
	 * @return the number of key-value pairs in this symbol table
	 */
	@View int size();

	/**
	 * Determines if this symbol table empty.
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
	 */
	@View V get(int key);

	/**
	 * Returns the value associated with the given key.
	 * 
	 * @param key the key
	 * @return the value associated with the given key if the key is in the symbol table.
	 *         Yields {@code _default} if the key is not in the symbol table
	 */
	@View V getOrDefault(int key, V _default);

	/**
	 * Yields the value associated with the given key.
	 * 
	 * @param key the key
	 * @return the value associated with the given key if the key is in the symbol table.
	 *         Yields {@code _default.get()} if the key is not in the symbol table
	 */
	V getOrDefault(int key, Supplier<? extends V> _default);

	/**
	 * Determines if this symbol table contains the given key (possibly bound to {@code null}).
	 * 
	 * @param key the key
	 * @return {@code true} if and only if this symbol table contains {@code key}
	 */
	@View boolean containsKey(int key);

	/**
	 * Yields the smallest key in the symbol table.
	 * 
	 * @return the smallest key in the symbol table
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	@View int min();

	/**
	 * Yields the largest key in the symbol table.
	 * 
	 * @return the largest key in the symbol table
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	@View int max();

	/**
	 * Yields the largest key in the symbol table less than or equal to {@code key}.
	 * 
	 * @param key the key
	 * @return the largest key in the symbol table less than or equal to {@code key}
	 * @throws NoSuchElementException if there is no such key
	 */
	@View int floorKey(int key);

	/**
	 * Yields the smallest key in the symbol table greater than or equal to {@code key}.
	 * 
	 * @param key the key
	 * @return the smallest key in the symbol table greater than or equal to {@code key}
	 * @throws NoSuchElementException if there is no such key
	 */
	@View int ceilingKey(int key);

	/**
	 * Yields the key in the symbol table whose rank is {@code k}.
	 * This is the (k+1)st smallest key in the symbol table. 
	 *
	 * @param  k the rank
	 * @return the key in the symbol table of rank {@code k}
	 * @throws IllegalArgumentException unless {@code k} is between 0 and {@code size()-1}
	 */
	@View int select(int k);

	/**
	 * Yields the number of keys in the symbol table strictly less than {@code key}.
	 * 
	 * @param key the key
	 * @return the number of keys in the symbol table strictly less than {@code key}
	 */
	@View int rank(int key);

	/**
	 * Yields an ordered stream of the entries (key/value) in this map, in
	 * increasing order of keys.
	 * 
	 * @return the stream
	 */
	Stream<Entry<V>> stream();

	/**
	 * Yields the keys of this map, in increasing order.
	 * 
	 * @return the keys
	 */
	List<Integer> keyList();

	/**
	 * Yields the ordered stream of the keys of this map, in increasing order.
	 * 
	 * @return the stream
	 */
	IntStream keys();

	/**
	 * Yields the ordered stream of the values of this map, in increasing order of corresponding key.
	 * 
	 * @return the stream
	 */
	Stream<V> values();

	/**
	 * Yields a snapshot of this map. The snapshot contains the elements in this map
	 * but is independent from this map: any future modification of this map will
	 * not be seen through the snapshot. A snapshot is always
	 * {@link io.takamaka.code.lang.Exported}.
	 * 
	 * @return a snapshot of this map
	 */
	StorageIntMapView<V> snapshot();
}