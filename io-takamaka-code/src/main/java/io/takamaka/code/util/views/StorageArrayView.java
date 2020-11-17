package io.takamaka.code.util.views;

import java.util.Iterator;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.util.StorageArray;

/**
 * A read-only view of a parent storage array. A view contains the same elements
 * as the parent storage array, but does not include modification methods.
 * Moreover, a view is exported, so that it can be safely divulged
 * outside the store of a node. Calls to the view are simply forwarded to
 * the parent array.
 *
 * @param <V> the type of the values
 */

@Exported
public class StorageArrayView<V> extends Storage implements StorageArray<V> {

	/**
	 * The parent array, whose elements are reflected in this view.
	 */
	private final StorageArray<V> parent;

	/**
	 * Builds a view of the given parent array. Any change in the parent array will be
	 * reflected in this view.
	 * 
	 * @param parent the reflected parent array
	 */
	public StorageArrayView(StorageArray<V> parent) {
		this.parent = parent;
	}

	@Override
	public Iterator<V> iterator() {
		return parent.iterator();
	}

	@Override
	public V get(int index) {
		return parent.get(index);
	}

	@Override
	public V getOrDefault(int index, V _default) {
		return parent.getOrDefault(index, _default);
	}

	@Override
	public V getOrDefault(int index, Supplier<? extends V> _default) {
		return parent.getOrDefault(index, _default);
	}

	@Override
	public Stream<V> stream() {
		return parent.stream();
	}

	@Override
	public <A> A[] toArray(IntFunction<A[]> generator) {
		return parent.toArray(generator);
	}

	@Override
	public String toString() {
		return parent.toString();
	}
}