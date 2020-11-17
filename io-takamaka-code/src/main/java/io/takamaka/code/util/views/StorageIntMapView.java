package io.takamaka.code.util.views;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageIntMap;

/**
 * A read-only view of a parent storage map. A view contains the same bindings
 * as the parent storage map, but does not include modification methods.
 * Moreover, a view is exported, so that it can be safely divulged outside
 * the store of a node. Calls to the view are simply forwarded to the parent map.
 *
 * @param <V> the type of the values
 */

@Exported
public class StorageIntMapView<V> extends Storage implements StorageIntMap<V> {

	/**
	 * The parent map, whose bindings are reflected in this view.
	 */
	private final StorageIntMap<V> parent;

	/**
	 * Builds a view of the given parent map. Any change in the parent map will be
	 * reflected in this view.
	 * 
	 * @param parent the reflected parent map
	 */
	public StorageIntMapView(StorageIntMap<V> parent) {
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
	public Iterator<Entry<V>> iterator() {
		return parent.iterator();
	}

	@Override
	public V get(int key) {
		return parent.get(key);
	}

	@Override
	public V getOrDefault(int key, V _default) {
		return parent.getOrDefault(key, _default);
	}

	@Override
	public V getOrDefault(int key, Supplier<? extends V> _default) {
		return parent.getOrDefault(key, _default);
	}

	@Override
	public boolean contains(int key) {
		return parent.contains(key);
	}

	@Override
	public int min() {
		return parent.min();
	}

	@Override
	public int max() {
		return parent.max();
	}

	@Override
	public int floorKey(int key) {
		return parent.floorKey(key);
	}

	@Override
	public int ceilingKey(int key) {
		return parent.ceilingKey(key);
	}

	@Override
	public int select(int k) {
		return parent.select(k);
	}

	@Override
	public int rank(int key) {
		return parent.rank(key);
	}

	@Override
	public String toString() {
		return parent.toString();
	}

	@Override
	public Stream<Entry<V>> stream() {
		return parent.stream();
	}

	@Override
	public List<Integer> keyList() {
		return parent.keyList();
	}

	@Override
	public IntStream keys() {
		return parent.keys();
	}
}