package io.takamaka.code.util.views;

import java.util.Iterator;
import java.util.stream.Stream;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageSet;

/**
 * A read-only view of a parent storage set. A view contains the same elements
 * as the parent storage set, but does not include modification methods.
 * Moreover, a view is exported, so that it can be safely divulged
 * outside the store of a node. Calls to the view are simply forwarded to
 * the parent set.
 *
 * @param <V> the type of the values
 */

@Exported
public class StorageSetView<V> extends Storage implements StorageSet<V> {

	/**
	 * The parent set, whose elements are reflected in this view.
	 */
	private final StorageSet<V> parent;

	/**
	 * Builds a view of the given parent set. Any change in the parent set will be
	 * reflected in this view.
	 * 
	 * @param parent the reflected parent set
	 */
	public StorageSetView(StorageSet<V> parent) {
		this.parent = parent;
	}

	@Override
	public @View int size() {
		return parent.size();
	}

	@Override
	public @View boolean isEmpty() {
		return parent.isEmpty();
	}

	@Override
	public @View boolean contains(Object value) {
		return parent.contains(value);
	}

	@Override
	public @View V min() {
		return parent.min();
	} 

	@Override
	public @View V max() {
		return parent.max();
	} 

	@Override
	public @View V floorKey(Object value) {
		return parent.floorKey(value);
	}    

	@Override
	public @View V ceilingKey(Object value) {
		return parent.ceilingKey(value);
	}

	@Override
	public @View V select(int k) {
		return parent.select(k);
	}

	@Override
	public @View int rank(Object value) {
		return parent.rank(value);
	} 

	@Override
	public String toString() {
		return parent.toString();
	}

	@Override
	public Iterator<V> iterator() {
		return parent.iterator();
	}

	@Override
	public Stream<V> stream() {
		return parent.stream();
	}
}