package takamaka.util;

import java.util.stream.IntStream;

import takamaka.lang.View;

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
	public @View int length();

	/**
	 * Yields the value at the given index.
	 * 
	 * @param index the index
	 * @return the value at the given index
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	public @View byte get(int index);

	/**
	 * Yields an ordered stream of the bytes in this array in increasing order of index.
	 * There is no specialized {@code ByteStream} class in the Java library, hence
	 * {@link java.util.stream.IntStream} is used instead, as best match.
	 * 
	 * @return the stream
	 */
	public IntStream stream();

	/**
	 * Yields an array containing the elements of this byte array.
	 * 
	 * @return the array
	 */
	public byte[] toArray();

	/**
	 * Determines the other object is a storage array of bytes with the same length and with
	 * the same elements at the same indexes.
	 * 
	 * @param other the other object
	 * @return true if and only if that condition holds
	 */
	@Override
	boolean equals(Object other);
}