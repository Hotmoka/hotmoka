package io.takamaka.code.util;

import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * An array of (possibly {@code null}) storage values, that can be kept in storage.
 * By iterating on this object, one gets the values of the array, in increasing index
 * order, including {@code null}s. This interface contains both read and modification methods.
 * 
 * @param <V> the type of the values
 */

public interface StorageArray<V> extends StorageArrayView<V> {

	/**
	 * Sets the value at the given index. This operation runs in logarithmic time.
	 *
	 * @param index the index
	 * @param value the value
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	void set(int index, V value);

	/**
	 * Replaces the old value {@code e} at {@code index} with {@code how.apply(e)}.
	 * If {@code index} was unmapped, it will be replaced with {@code how.apply(null)},
	 * which might well lead to a run-time exception.
	 *
	 * @param index the index whose value must be replaced
	 * @param how the replacement function
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	void update(int index, UnaryOperator<V> how);

	/**
	 * Replaces the old value {@code e} at {@code index} with {@code how.apply(e)}.
	 * If {@code index} was unmapped, it will be replaced with {@code how.apply(_default)}.
	 *
	 * @param index the index whose value must be replaced
	 * @param _default the default value
	 * @param how the replacement function
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	void update(int index, V _default, UnaryOperator<V> how);

	/**
	 * Replaces the old value {@code e} at {@code index} with {@code how.apply(e)}.
	 * If {@code index} was unmapped, it will be replaced with {@code how.apply(_default.get())}.
	 *
	 * @param index the index whose value must be replaced
	 * @param _default the supplier of the default value
	 * @param how the replacement function
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	void update(int index, Supplier<? extends V> _default, UnaryOperator<V> how);

	/**
	 * If the given index is unmapped or is mapped to {@code null}, map it to the given value.
	 * 
	 * @param index the index
	 * @param value the value
	 * @return the previous value at the given index. Yields {@code null} if {@code index} was previously unmapped
	 *         or was mapped to {@code null}
	 */
	V setIfAbsent(int index, V value);

	/**
	 * If the given index is unmapped or is mapped to {@code null}, map it to the value given by a supplier.
	 * 
	 * @param index the index
	 * @param supplier the supplier
	 * @return the previous value at the given index, if it was already mapped to a non-{@code null} value.
	 *         If the index was unmapped or was mapped to {@code null}, yields the new value
	 */
	V computeIfAbsent(int index, Supplier<? extends V> supplier);

	/**
	 * If the given index is unmapped or is mapped to {@code null}, map it to the value given by a supplier.
	 * 
	 * @param index the index
	 * @param supplier the supplier
	 * @return the previous value at the given index, if it was already mapped to a non-{@code null} value.
	 *         If the index was unmapped or was mapped to {@code null}, yields the new value
	 */
	V computeIfAbsent(int index, IntFunction<? extends V> supplier);

	/**
	 * Yields a view of this array. The view reflects the elements in this array:
	 * any future modification of this array will be seen also through the view.
	 * A view is always {@link io.takamaka.code.lang.Exported}.
	 * 
	 * @return a view of this array
	 */
	StorageArrayView<V> view();

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