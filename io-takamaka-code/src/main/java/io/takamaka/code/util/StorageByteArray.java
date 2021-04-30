/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

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