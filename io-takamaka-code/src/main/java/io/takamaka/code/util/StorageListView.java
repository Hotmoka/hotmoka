package io.takamaka.code.util;

import java.util.NoSuchElementException;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import io.takamaka.code.lang.View;

/**
 * A list of elements. It is possible to access elements at both sides of the list.
 * A list can hold {@code null} elements.
 * This interface has only access methods. Its sub-interface
 * {@link StorageList} includes modification methods as well.
 *
 * @param <E> the type of the elements. This type must be allowed in storage
 */
public interface StorageListView<E> extends Iterable<E> {

	/**
	 * Returns true if this list contains the specified element. More formally, returns true
	 * if and only if this list contains at least one element {@code e} such that
	 * {@code (o==null ? e==null : o.equals(e))}.
	 *
	 * @param e element whose presence in this list is to be tested, possibly {@code null}
	 * @return true if and only if this list contains the specified element
	 */
	@View boolean contains(Object e);

	/**
	 * Yields the first element of this list, if any.
	 * 
	 * @return the first element
	 * @throws NoSuchElementException if this list is empty
	 */
	@View E first();

	/**
	 * Yields the last element of this list, if any.
	 * 
	 * @return the last element
	 * @throws NoSuchElementException if this list is empty
	 */
	@View E last();

	/**
	 * Yields the element of this list at position {@code index}.
	 * 
	 * @param index the index of the element, between 0 (inclusive) and {@code size() - 1} (exclusive)
	 * @return the element at the given index
	 * @throws IndexOutOfBoundsException if the index is negative or equal or greater than
	 *                                   the size of this list
	 */
	@View E get(int index);

	/**
	 * Yields the size of this list.
	 * 
	 * @return the size of this list
	 */
	@View int size();

	/**
	 * Yields an ordered (first to last) stream of the elements of this list.
	 * 
	 * @return the stream
	 */
	Stream<E> stream();

	/**
	 * Yields an array containing the elements of this list, in their order in the list,
	 * using the provided generator function to allocate the returned array.
	 * 
	 * @param generator the array generator
	 * @return the array
	 * @throws ArrayStoreException if the runtime type of the array returned from the array generator
	 *                             is not a supertype of the runtime type of every element in this list
	 */
	<A> A[] toArray(IntFunction<A[]> generator);

	/**
	 * Yields a snapshot of this list. The snapshot contains the elements in this list
	 * but is independent from this list: any future modification of this list will
	 * not be seen through the snapshot. A snapshot is always
	 * {@link io.takamaka.code.lang.Exported}.
	 * 
	 * @return a snapshot of this list
	 */
	StorageListView<E> snapshot();
}