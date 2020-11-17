package io.takamaka.code.util.internal;

import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.util.ModifiableStorageIntMap;

/**
 * A view of a parent storage map. A view contains the same bindings
 * as the parent storage map. Moreover, a view is exported, so that
 * it can be safely divulged outside the store of a node. Calls to the view
 * are simply forwarded to the parent map.
 *
 * @param <V> the type of the values
 */

@Exported
public class ModifiableStorageIntMapView<V> extends StorageIntMapView<V> implements ModifiableStorageIntMap<V> {

	/**
	 * Builds a view of the given parent map. Any change to the parent map will be
	 * reflected in this view and vice versa.
	 * 
	 * @param parent the reflected parent map
	 */
	public ModifiableStorageIntMapView(ModifiableStorageIntMap<V> parent) {
		super(parent);
	}

	@Override
	protected ModifiableStorageIntMap<V> getParent() {
		return (ModifiableStorageIntMap<V>) super.getParent();
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
	public void put(int key, V value) {
		getParent().put(key, value);
	}

	@Override
	public void remove(int key) {
		getParent().remove(key);
	}

	@Override
	public void update(int key, UnaryOperator<V> how) {
		getParent().update(key, how);
	}

	@Override
	public void update(int key, V _default, UnaryOperator<V> how) {
		getParent().update(key, how);
	}

	@Override
	public void update(int key, Supplier<V> _default, UnaryOperator<V> how) {
		getParent().update(key, how);
	}

	@Override
	public V putIfAbsent(int key, V value) {
		return getParent().putIfAbsent(key, value);
	}

	@Override
	public V computeIfAbsent(int key, Supplier<V> supplier) {
		return getParent().computeIfAbsent(key, supplier);
	}

	@Override
	public V computeIfAbsent(int key, IntFunction<V> supplier) {
		return getParent().computeIfAbsent(key, supplier);
	}
}