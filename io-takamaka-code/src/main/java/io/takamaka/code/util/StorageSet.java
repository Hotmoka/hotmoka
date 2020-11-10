package io.takamaka.code.util;

import java.util.NoSuchElementException;
import java.util.stream.Stream;

import io.takamaka.code.lang.View;
import io.takamaka.code.util.internal.StorageSetView;

/**
 * A sorted set of (non-{@code null}) storage values.
 * By iterating on this object, one gets the values in the set, in increasing order.
 * This interface has only access methods. Its sub-interface
 * {@link #io.takamaka.code.util.ModifiableStorageSet} includes
 * modification methods as well.
 * 
 * @param <V> the type of the values
 */

public interface StorageSet<V> extends Iterable<V> {

	/**
	 * Yields a view of the given parent set. All changes in the parent set
	 * are reflected in the view. The parent set cannot be modified
	 * through the view, since the latter misses any modification method.
	 * 
	 * @param <V> the type of the elements of the view
	 * @param parent the parent set
	 * @return the resulting view
	 */
	static <V> StorageSet<V> viewOf(StorageSet<V> parent) {
		return new StorageSetView<>(parent);
	}

	/**
	 * Returns the number of values in this set.
	 * 
	 * @return the number of values in this set
	 */
	@View int size();

	/**
	 * Determines if this set is empty.
	 * 
	 * @return {@code true} if and only if this set is empty
	 */
	@View boolean isEmpty();

	/**
	 * Determines if this set contains the given value.
	 * 
	 * @param value the value
	 * @return {@code true} if and only if this set contains {@code value}
	 * @throws IllegalArgumentException if {@code value} is {@code null}
	 */
	@View boolean contains(Object value);

	/**
	 * Yields the smallest value in this set.
	 * 
	 * @return the smallest value in this set
	 * @throws NoSuchElementException if the set is empty
	 */
	@View V min();

	/**
	 * Yields the largest value in this set.
	 * 
	 * @return the largest value in this set
	 * @throws NoSuchElementException if the set is empty
	 */
	@View V max();

	/**
	 * Yields the largest value in this set less than or equal to {@code value}.
	 * 
	 * @param value the reference value
	 * @return the largest value in this set less than or equal to {@code value}
	 * @throws NoSuchElementException if there is no such value
	 * @throws IllegalArgumentException if {@code value} is {@code null}
	 */
	@View V floorKey(Object value);

	/**
	 * Yields the smallest value in this set greater than or equal to {@code value}.
	 * 
	 * @param value the value
	 * @return the smallest value in this set greater than or equal to {@code value}
	 * @throws NoSuchElementException if there is no such value
	 * @throws IllegalArgumentException if {@code value} is {@code null}
	 */
	@View V ceilingKey(Object value);

	/**
	 * Yields the value in this set whose rank is {@code k}.
	 * This is the (k+1)st smallest value in this set. 
	 *
	 * @param  k the rank
	 * @return the value in this set, of rank {@code k}
	 * @throws IllegalArgumentException unless {@code k} is between 0 and {@code size()-1}
	 */
	@View V select(int k);

	/**
	 * Yields the number of values in this set strictly less than {@code value}.
	 * 
	 * @param value the value
	 * @return the number of values in this set strictly less than {@code value}
	 * @throws IllegalArgumentException if {@code value} is {@code null}
	 */
	@View int rank(Object value);

	/**
	 * Yields an ordered stream of the values in this set, in increasing order.
	 * 
	 * @return the stream
	 */
	Stream<V> stream();
}