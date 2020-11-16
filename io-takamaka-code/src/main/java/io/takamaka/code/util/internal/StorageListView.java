package io.takamaka.code.util.internal;

import java.util.Iterator;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageList;

/**
 * A read-only view of a parent storage list. A view contains the same elements
 * as the parent storage list, but does not include modification methods.
 * Moreover, a view is exported, so that it can be safely divulged
 * outside the store of a node. Calls to the view are simply forwarded to
 * the parent list.
 *
 * @param <V> the type of the values
 */

@Exported
public class StorageListView<V> extends Storage implements StorageList<V> {

	/**
	 * The parent list, whose elements are reflected in this view.
	 */
	private final StorageList<V> parent;

	/**
	 * Builds a view of the given parent list. Any change in the parent list will be
	 * reflected in this view.
	 * 
	 * @param parent the reflected parent list
	 */
	public StorageListView(StorageList<V> parent) {
		this.parent = parent;
	}

	/**
	 * Yields the reflected list.
	 * 
	 * @return the reflected list
	 */
	protected StorageList<V> getParent() {
		return parent;
	}

	@Override
	public @View int size() {
		return parent.size();
	}

	@Override
	public @View boolean contains(Object value) {
		return parent.contains(value);
	}

	@Override
	public Iterator<V> iterator() {
		return parent.iterator();
	}

	@Override
	public Stream<V> stream() {
		return parent.stream();
	}

	@Override
	public V first() {
		return parent.first();
	}

	@Override
	public V last() {
		return parent.last();
	}

	@Override
	public V get(int index) {
		return parent.get(index);
	}

	@Override
	public <A> A[] toArray(IntFunction<A[]> generator) {
		return parent.toArray(generator);
	}
}