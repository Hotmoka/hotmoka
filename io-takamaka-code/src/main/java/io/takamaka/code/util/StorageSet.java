package io.takamaka.code.util;

import java.util.NoSuchElementException;

/**
 * A sorted set of (non-{@code null}) storage values,
 * that can be kept in storage. By iterating on this object, one gets
 * the values in the set, in increasing order. This interface includes
 * read and modification methods.
 * 
 * @param <V> the type of the values. This type must be allowed in storage
 */

public interface StorageSet<V> extends StorageSetView<V> {

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

	/**
	 * Yields a view of this set. The view reflects the elements in this set:
	 * any future modification of this set will be seen also through the view.
	 * A view is always {@link io.takamaka.code.lang.Exported}.
	 * 
	 * @return a view of this set
	 */
	StorageSetView<V> view();

	/**
	 * Yields a snapshot of this set. The snapshot contains the elements in this set
	 * but is independent from this set: any future modification of this set will
	 * not be seen through the snapshot. A snapshot is always
	 * {@link io.takamaka.code.lang.Exported}.
	 * 
	 * @return a snapshot of this set
	 */
	StorageSetView<V> snapshot();
}