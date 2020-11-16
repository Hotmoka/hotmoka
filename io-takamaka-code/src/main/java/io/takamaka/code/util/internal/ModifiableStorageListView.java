package io.takamaka.code.util.internal;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.util.ModifiableStorageList;

/**
 * A view of a parent storage list. A view contains the same elements
 * as the parent storage list. Moreover, a view is exported, so that it can
 * be safely divulged outside the store of a node. Calls to the view are
 * simply forwarded to the parent list.
 *
 * @param <V> the type of the elements of the list
 */

@Exported
public class ModifiableStorageListView<V> extends StorageListView<V> implements ModifiableStorageList<V> {

	/**
	 * Builds a view of the given parent list. Any change to the parent list will be
	 * reflected in this view and vice versa.
	 * 
	 * @param parent the reflected parent list
	 */
	public ModifiableStorageListView(ModifiableStorageList<V> parent) {
		super(parent);
	}

	@Override
	protected ModifiableStorageList<V> getParent() {
		return (ModifiableStorageList<V>) super.getParent();
	}

	@Override
	public void addFirst(V element) {
		getParent().addFirst(element);
	}

	@Override
	public void addLast(V element) {
		getParent().addLast(element);
	}

	@Override
	public void add(V element) {
		getParent().add(element);
	}

	@Override
	public void clear() {
		getParent().clear();
	}

	@Override
	public V removeFirst() {
		return getParent().removeFirst();
	}

	@Override
	public boolean remove(Object e) {
		return getParent().remove(e);
	}
}