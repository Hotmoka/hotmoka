package io.takamaka.code.util.views;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMap;

/**
 * A read-only view of a parent storage map. A view contains the same bindings
 * as the parent storage map, but does not include modification methods.
 * Moreover, a view is exported, so that it can be safely divulged outside
 * the store of a node. Calls to the view are simply forwarded to the parent map.
 *
 * @param <K> the type of the keys
 * @param <V> the type of the values
 */

@Exported
public class StorageMapView<K,V> extends Storage implements StorageMap<K,V> {

	/**
	 * The parent map, whose bindings are reflected in this view.
	 */
	private final StorageMap<K,V> parent;

	/**
	 * Builds a view of the given parent map. Any change in the parent map will be
	 * reflected in this view.
	 * 
	 * @param parent the reflected parent map
	 */
	public StorageMapView(StorageMap<K,V> parent) {
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
	public Iterator<Entry<K, V>> iterator() {
		return parent.iterator();
	}

	@Override
	public V get(Object key) {
		return parent.get(key);
	}

	@Override
	public V getOrDefault(Object key, V _default) {
		return parent.getOrDefault(key, _default);
	}

	@Override
	public V getOrDefault(Object key, Supplier<? extends V> _default) {
		return parent.getOrDefault(key, _default);
	}

	@Override
	public K min() {
		return parent.min();
	}

	@Override
	public K max() {
		return parent.max();
	}

	@Override
	public K floorKey(K key) {
		return parent.floorKey(key);
	}

	@Override
	public K ceilingKey(K key) {
		return parent.ceilingKey(key);
	}

	@Override
	public K select(int k) {
		return parent.select(k);
	}

	@Override
	public int rank(K key) {
		return parent.rank(key);
	}

	@Override
	public Stream<Entry<K, V>> stream() {
		return parent.stream();
	}

	@Override
	public List<K> keyList() {
		return parent.keyList();
	}

	@Override
	public Stream<K> keys() {
		return parent.keys();
	}
}