package io.takamaka.code.util;

import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.takamaka.code.lang.View;

/**
 * An array of (possibly {@code null}) storage values, that can be kept in storage.
 * By iterating on this object, one gets the values of the array, in increasing index
 * order, including {@code null}s. This interface contains read methods only.
 * The class {@link StorageArray} includes
 * modification methods as well.
 * 
 * @param <V> the type of the values
 */

public interface StorageArrayView<V> extends Iterable<V> {

	/**
	 * Yields the length of this array.
	 * 
	 * @return the length of this array
	 */
	@View int length();

	/**
	 * Yields the value at the given index, if any. This operation runs in logarithmic time.
	 * 
	 * @param index the index
	 * @return the value at the given index if the index has been assigned to a value
	 *         and {@code null} otherwise
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	@View V get(int index);

	/**
	 * Yields the value at the given index, if any. This operation runs in logarithmic time.
	 * 
	 * @param index the index
	 * @return the value at the given index if the index has been assigned to a value
	 *         and {@code _default} otherwise
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	@View V getOrDefault(int index, V _default);

	/**
	 * Yields the value at the given index, if any. This operation runs in logarithmic time.
	 * 
	 * @param index the index
	 * @return the value at the given index if the index has been assigned to a value
	 *         and {@code _default.get()} otherwise
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	V getOrDefault(int index, Supplier<? extends V> _default);

	/**
	 * Yields an ordered stream of the values in this array (including {@code null}s),
	 * in increasing order of index.
	 * 
	 * @return the stream
	 */
	Stream<V> stream();

	/**
	 * Yields an array containing the elements of this storage array, in their order,
	 * using the provided generator function to allocate the returned array.
	 * 
	 * @param generator the array generator
	 * @return the array
	 * @throws ArrayStoreException if the runtime type of the array returned from the array generator
	 *                             is not a supertype of the runtime type of every element in this storage array
	 */
	<A> A[] toArray(IntFunction<A[]> generator);

	/**
	 * Yields a snapshot of this array. The snapshot contains the elements in this array
	 * but is independent from this array: any future modification of this array will
	 * not be seen through the snapshot. A snapshot is always
	 * {@link io.takamaka.code.lang.Exported}.
	 * 
	 * @return a snapshot of this array
	 */
	StorageArrayView<V> snapshot();
}