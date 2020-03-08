package io.takamaka.code.util;

import java.util.stream.IntStream;

import io.takamaka.code.lang.View;

/**
 * An array of byte values. Unset elements default to 0.
 * By iterating on this object, one gets the values of the array, in increasing index order.
 */

public interface ByteArray extends Iterable<Byte> {

	/**
	 * Yields the length of this array.
	 * 
	 * @return the length of this array
	 */
	@View int length();

	/**
	 * Yields the value at the given index.
	 * 
	 * @param index the index
	 * @return the value at the given index
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	@View byte get(int index);

	/**
	 * Yields an ordered stream of the bytes in this array in increasing order of index.
	 * There is no specialized {@code ByteStream} class in the Java library, hence
	 * {@link java.util.stream.IntStream} is used instead, as best match.
	 * 
	 * @return the stream
	 */
	IntStream stream();

	/**
	 * Yields an array containing the elements of this byte array.
	 * This object is not backed by the returned array, meaning that
	 * subsequent updates to the returned array do not affect this object.
	 * 
	 * @return the array
	 */
	byte[] toArray();

	/**
	 * Determines if the other object is a storage array of bytes with the same length and with
	 * the same elements in the same order. No other aspect of the arrays is checked.
	 * In particular, mutable and immutable are considered equal as long as the
	 * condition above holds.
	 * 
	 * @param other the other object
	 * @return true if and only if that condition holds
	 */
	@Override @View
	boolean equals(Object other);

	/**
	 * Yields the hash code for this array. It considers its elements and their order only.
	 * In particular, it does not consider if the array is mutable or immutable,
	 * in order to be compatible with {@link io.takamaka.code.util.ByteArray#equals(Object)}.
	 * 
	 * @return true if and only if the above condition holds
	 */
	@Override @View
	int hashCode();
}