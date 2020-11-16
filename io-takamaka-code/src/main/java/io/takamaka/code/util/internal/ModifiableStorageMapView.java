package io.takamaka.code.util.internal;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.util.ModifiableStorageMap;

/**
 * A view of a parent storage map. A view contains the same bindings
 * as the parent storage map. Moreover, a view is exported, so that
 * it can be safely divulged outside the store of a node. Calls to the view
 * are simply forwarded to the parent map.
 *
 * @param <K> the type of the keys
 * @param <V> the type of the values
 */

@Exported
public class ModifiableStorageMapView<K,V> extends StorageMapView<K,V> implements ModifiableStorageMap<K,V> {

	/**
	 * Builds a view of the given parent map. Any change to the parent map will be
	 * reflected in this view and vice versa.
	 * 
	 * @param parent the reflected parent map
	 */
	public ModifiableStorageMapView(ModifiableStorageMap<K,V> parent) {
		super(parent);
	}

	@Override
	protected ModifiableStorageMap<K,V> getParent() {
		return (ModifiableStorageMap<K,V>) super.getParent();
	}

	@Override
	public void put(K key, V value) {
		getParent().put(key, value);
	}

	@Override
	public V putIfAbsent(K key, V value) {
		return getParent().putIfAbsent(key, value);
	}

	@Override
	public V computeIfAbsent(K key, Supplier<V> supplier) {
		return getParent().computeIfAbsent(key, supplier);
	}

	@Override
	public V computeIfAbsent(K key, Function<K, V> supplier) {
		return getParent().computeIfAbsent(key, supplier);
	}

	@Override
	public void removeMin() {
		getParent().removeMin();
	}

	@Override
	public void removeMax() {
		getParent().removeMax();
	}

	@Override
	public void remove(Object key) {
		getParent().remove(key);
	}

	@Override
	public void update(K key, UnaryOperator<V> how) {
		getParent().update(key, how);
	}

	@Override
	public void update(K key, V _default, UnaryOperator<V> how) {
		getParent().update(key, _default, how);
	}

	@Override
	public void update(K key, Supplier<V> _default, UnaryOperator<V> how) {
		getParent().update(key, _default, how);
	}
}