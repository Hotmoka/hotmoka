package io.takamaka.code.util;

import java.util.function.IntUnaryOperator;

/**
 * A mutable array of byte values. Unset elements default to 0.
 * By iterating on this object, one gets the values of the array, in increasing index order.
 */

public interface MutableByteArray extends ByteArray {

	/**
	 * Sets the value at the given index.
	 *
	 * @param index the index
	 * @param value the value
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	public void set(int index, byte value);

	/**
	 * Replaces the old value {@code e} at {@code index} with {@code (byte) how.applyAsInt(e)}.
	 *
	 * @param index the index whose value must be replaced
	 * @param how the replacement function
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	public void update(int index, IntUnaryOperator how);
}