package io.takamaka.code.util;

import java.util.function.IntUnaryOperator;

/**
 * A mutable array of byte values. Unset elements default to 0.
 * By iterating on this object, one gets the values of the array, in increasing index order.
 */

public interface StorageByteArray extends StorageByteArrayView {

	/**
	 * Sets the value at the given index.
	 *
	 * @param index the index
	 * @param value the value
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	void set(int index, byte value);

	/**
	 * Replaces the old value {@code e} at {@code index} with {@code (byte) how.applyAsInt(e)}.
	 *
	 * @param index the index whose value must be replaced
	 * @param how the replacement function
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	void update(int index, IntUnaryOperator how);

	/**
	 * Yields a view of this array. The view reflects the elements in this array:
	 * any future modification of this array will be seen also through the view.
	 * A view is always {@link io.takamaka.code.lang.Exported}.
	 * 
	 * @return a view of this array
	 */
	StorageByteArrayView view();
}