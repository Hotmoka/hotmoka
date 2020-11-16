package io.takamaka.code.util;

import java.util.Collection;
import java.util.NoSuchElementException;

import io.takamaka.code.util.internal.ModifiableStorageListImpl;
import io.takamaka.code.util.internal.ModifiableStorageListView;

/**
 * A list of elements. It is possible to access elements at both sides of the list.
 * A list can hold {@code null} elements.
 * This interface has access methods and modification methods.
 *
 * @param <E> the type of the elements. This type must be allowed in storage
 */
public interface ModifiableStorageList<E> extends StorageList<E> {

	/**
	 * Yields an empty list.
	 * 
	 * @return the empty list
	 */
	static <V> ModifiableStorageList<V> empty() {
		return new ModifiableStorageListImpl<>();
	}

	/**
	 * Yields a list initialized to the same elements as the given parent collection.
	 * 
	 * @param parent the parent collection
	 * @return the list
	 */
	static <V> ModifiableStorageList<V> of(Collection<? extends V> parent) {
		return new ModifiableStorageListImpl<V>(parent);
	}

	/**
	 * Yields an exported view of the given parent list. All changes in the parent list
	 * are reflected in the view and vice versa.
	 * 
	 * @param <V> the type of the elements of the view
	 * @param parent the parent list
	 * @return the resulting view
	 */
	static <V> ModifiableStorageList<V> viewOf(ModifiableStorageList<V> parent) {
		return new ModifiableStorageListView<>(parent);
	}

	/**
	 * Adds the given element as first element of this list.
	 * 
	 * @param element the element, possibly {@code null}
	 */
	void addFirst(E element);

	/**
	 * Adds the given element as last element of this list.
	 * 
	 * @param element the element, possibly {@code null}
	 */
	void addLast(E element);

	/**
	 * Adds the given element as first element of this list.
	 * This is synonym of {@link io.takamaka.code.util.ModifiableStorageList#addLast(E)}.
	 * 
	 * @param element the element, possibly {@code null}
	 */
	void add(E element);

	/**
	 * Clears this list, removing all its elements.
	 */
	void clear();

	/**
	 * Removes and yields the first element of this list, if any.
	 * 
	 * @return the first element, removed from this list
	 * @throws NoSuchElementException if this list is empty
	 */
	E removeFirst();

	/**
	 * Removes the first occurrence of the specified element from this list, if it is present.
	 * If this list does not contain the element, it is unchanged. More formally, removes
	 * the element with the lowest index {@code i} such that
	 * {@code e==null ? get(i)==null : e.equals(get(i))}
	 * (if such an element exists). Returns true if this list contained the specified
	 * element (or equivalently, if this list changed as a result of the call).
	 * 
	 * @param e the element to remove, possibly {@code null}
	 * @return true if and only if the list was modified as result of this call
	 */
	boolean remove(Object e);
}