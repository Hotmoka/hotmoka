package io.takamaka.code.util;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.takamaka.code.util.internal.ModifiableStorageIntMapImpl;
import io.takamaka.code.util.internal.ModifiableStorageIntMapView;

/**
 * A map from integer keys to (possibly {@code null}) storage values,
 * that can be kept in storage. By iterating on this object, one gets
 * the key/value pairs of the map, in increasing key order. This interface includes
 * read and modification methods.
 * 
 * @param <V> the type of the values
 */

public interface ModifiableStorageIntMap<V> extends StorageIntMap<V> {

	/**
	 * Yields an empty map.
	 * 
	 * @return the empty map
	 */
	static <V> ModifiableStorageIntMap<V> empty() {
		return new ModifiableStorageIntMapImpl<>();
	}

	/**
	 * Yields a map initialized to the same bindings as the given parent map.
	 * 
	 * @param parent the parent map
	 * @return the map
	 */
	static <V> ModifiableStorageIntMap<V> of(Map<Integer, ? extends V> parent) {
		return new ModifiableStorageIntMapImpl<V>(parent);
	}

	/**
	 * Yields an exported view of the given parent map. All changes in the parent map
	 * are reflected in the view and vice versa.
	 * 
	 * @param <V> the type of the values of the view
	 * @param parent the parent map
	 * @return the resulting view
	 */
	static <V> ModifiableStorageIntMap<V> viewOf(ModifiableStorageIntMap<V> parent) {
		return new ModifiableStorageIntMapView<>(parent);
	}

	/**
	 * Inserts the specified key-value pair into this symbol table, overwriting the old 
	 * value with the new value if the symbol table already contains the specified key.
	 *
	 * @param key the key
	 * @param value the value
	 */
	void put(int key, V value);

	/**
	 * Removes the smallest key and associated value from the symbol table.
	 * 
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	void removeMin();

	/**
	 * Removes the largest key and associated value from the symbol table.
	 * 
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	void removeMax();

	/**
	 * Removes the specified key and its associated value from this symbol table     
	 * (if the key is in this symbol table).    
	 *
	 * @param  key the key
	 */
	void remove(int key);

	/**
	 * Replaces the old value {@code e} at {@code key} with {@code how.apply(e)}.
	 * If {@code key} was unmapped, it will be replaced with {@code how.apply(null)},
	 * which might well lead to a run-time exception.
	 *
	 * @param key the key whose value must be replaced
	 * @param how the replacement function
	 */
	void update(int key, UnaryOperator<V> how);

	/**
	 * Replaces the old value {@code e} at {@code key} with {@code how.apply(e)}.
	 * If {@code key} was unmapped, it will be replaced with {@code how.apply(_default)}.
	 *
	 * @param key the key whose value must be replaced
	 * @param _default the default value
	 * @param how the replacement function
	 */
	void update(int key, V _default, UnaryOperator<V> how);

	/**
	 * Replaces the old value {@code e} at {@code key} with {@code how.apply(e)}.
	 * If {@code key} was unmapped, it will be replaced with {@code how.apply(_default.get())}.
	 *
	 * @param key the key whose value must be replaced
	 * @param _default the supplier of the default value
	 * @param how the replacement function
	 */
	void update(int key, Supplier<V> _default, UnaryOperator<V> how);

	/**
	 * If the given key is unmapped or is mapped to {@code null}, map it to the given value.
	 * 
	 * @param key the key
	 * @param value the value
	 * @return the previous value at the given key. Yields {@code null} if {@code key} was previously unmapped
	 *         or was mapped to {@code null}
	 */
	V putIfAbsent(int key, V value);

	/**
	 * If the given key is unmapped or is mapped to {@code null}, map it to the value given by a supplier.
	 * 
	 * @param key the key
	 * @param supplier the supplier
	 * @return the previous value at the given key, if it was already mapped to a non-{@code null} value.
	 *         If the key was unmapped or was mapped to {@code null}, yields the new value
	 */
	V computeIfAbsent(int key, Supplier<V> supplier);

	/**
	 * If the given key is unmapped or is mapped to {@code null}, map it to the value given by a supplier.
	 * 
	 * @param key the key
	 * @param supplier the supplier
	 * @return the previous value at the given key, if it was already mapped to a non-{@code null} value.
	 *         If the key was unmapped or was mapped to {@code null}, yields the new value
	 */
	V computeIfAbsent(int key, IntFunction<V> supplier);
}