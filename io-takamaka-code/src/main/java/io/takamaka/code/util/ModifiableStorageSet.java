package io.takamaka.code.util;

import java.util.Collection;
import java.util.NoSuchElementException;

import io.takamaka.code.util.internal.ModifiableStorageSetImpl;

/**
 * A sorted set of (non-{@code null}) storage values,
 * that can be kept in storage. By iterating on this object, one gets
 * the values in the set, in increasing order. This interface includes
 * read and modificatoin methods.
 * 
 * @param <V> the type of the values
 */

public interface ModifiableStorageSet<V> extends StorageSet<V> {

	/**
	 * Yields an empty set.
	 * 
	 * @return the empty set
	 */
	static <V> ModifiableStorageSet<V> empty() {
		return new ModifiableStorageSetImpl<>();
	}

	/**
	 * Yields a set initialized to the same elements as the given parent collection.
	 * 
	 * @param parent the parent collection
	 * @return the set
	 */
	static <V> ModifiableStorageSet<V> of(Collection<? extends V> parent) {
		return new ModifiableStorageSetImpl<V>(parent);
	}

	/**
	 * Adds the specified value into this set, if it is not already there.
	 *
	 * @param value the value
	 * @throws IllegalArgumentException if {@code value} is {@code null}
	 */
	void add(V value);

	/**
	 * Removes the smallest value from this set.
	 * 
	 * @throws NoSuchElementException if this set is empty
	 */
	void removeMin();

	/**
	 * Removes the largest value from this set.
	 * 
	 * @throws NoSuchElementException if this set is empty
	 */
	void removeMax();

	/**
	 * Removes the specified value from this set, if it is there.
	 *
	 * @param  value the value
	 * @throws IllegalArgumentException if {@code value} is {@code null}
	 */
	void remove(Object value);
}