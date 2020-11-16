package io.takamaka.code.util.internal;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.util.ModifiableStorageSet;

/**
 * A view of a parent storage set. A view contains the same elements
 * as the parent storage set. Moreover, a view is exported, so that it can
 * be safely divulged outside the store of a node. Calls to the view are
 * simply forwarded to the parent set.
 *
 * @param <V> the type of the elements of the set
 */

@Exported
public class ModifiableStorageSetView<V> extends StorageSetView<V> implements ModifiableStorageSet<V> {

	/**
	 * Builds a view of the given parent set. Any change to the parent set will be
	 * reflected in this view and vice versa.
	 * 
	 * @param parent the reflected parent set
	 */
	public ModifiableStorageSetView(ModifiableStorageSet<V> parent) {
		super(parent);
	}

	@Override
	protected ModifiableStorageSet<V> getParent() {
		return (ModifiableStorageSet<V>) super.getParent();
	}

	@Override
	public void add(V value) {
		getParent().add(value);
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
	public void remove(Object value) {
		getParent().remove(value);
	}
}