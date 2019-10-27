package io.takamaka.code.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

/**
 * A list of elements that can be kept in storage. It is possible to
 * add and access elements at both sides of the list. This list can hold
 * {@code null} elements.
 *
 * @param <E> the type of the elements. This type must be allowed in storage
 */
public class StorageList<E> extends Storage implements Iterable<E> {

	/**
	 * The first node of the list.
	 */
	private Node<E> first;

	/**
	 * The last node of the list.
	 */
	private Node<E> last;

	/**
	 * The size of the list.
	 */
	private int size;

	/**
	 * A node of the list.
	 * 
	 * @param <E> the type of the element inside the node
	 */
	private static class Node<E> extends Storage {

		/**
		 * The element inside the node.
		 */
		private final E element;

		/**
		 * The next node.
		 */
		private Node<E> next;

		/**
		 * Builds the node.
		 * 
		 * @param element the element to put inside the node
		 */
		private Node(E element) {
			this.element = element;
		}

		/**
		 * Builds the node.
		 * 
		 * @param element the element to put inside the node
		 * @param next the next node
		 */		
		private Node(E element, Node<E> next) {
			this.element = element;
			this.next = next;
		}
	}

	/**
	 * Adds the given element as first element of this list.
	 * 
	 * @param element the element, possibly {@code null}
	 */
	public void addFirst(E element) {
		if (first == null)
			first = last = new Node<>(element);
		else
			first = new Node<>(element, first);

		size++;
	}

	/**
	 * Adds the given element as last element of this list.
	 * 
	 * @param element the element, possibly {@code null}
	 */
	public void addLast(E element) {
		if (last == null)
			first = last = new Node<>(element);
		else
			last = last.next = new Node<>(element);

		size++;
	}

	/**
	 * Adds the given element as first element of this list.
	 * This is synonym of {@link io.takamaka.code.util.StorageList#addFirst(Object)}.
	 * 
	 * @param element the element, possibly {@code null}
	 */
	public void add(E element) {
		addLast(element);
	}

	/**
	 * Clears this list, removing all its elements.
	 */
	public void clear() {
		first = last = null;
		size = 0;
	}

	/**
	 * Removes and yields the first element of this list, if any.
	 * 
	 * @return the first element, removed from this list
	 * @throws NoSuchElementException if this list is empty
	 */
	public E removeFirst() {
		if (first == null)
			throw new NoSuchElementException();
		else {
			E element = first.element;

			if (first == last)
				first = last = null;
			else
				first = first.next;

			size--;
			return element;
		}
	}

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
	public boolean remove(Object e) {
		for (Node<E> cursor = first, previous = null; cursor != null; previous = cursor, cursor = cursor.next) {
			E element = cursor.element;
			if (e == null ? element == null : e.equals(element)) {
				if (last == cursor)
					last = previous;

				if (first == cursor)
					first = cursor.next;

				if (previous != null)
					previous.next = cursor.next;

				size--;

				return true;
			}
		}

		return false;
	}

	/**
	 * Returns true if this list contains the specified element. More formally, returns true
	 * if and only if this list contains at least one element {@code e} such that
	 * {@code (o==null ? e==null : o.equals(e))}.
	 *
	 * @param e element whose presence in this list is to be tested, possibly {@code null}
	 * @return true if and only if this list contains the specified element
	 */
	public @View boolean contains(Object e) {
		for (Node<E> cursor = first; cursor != null; cursor = cursor.next) {
			E element = cursor.element;
			if (e == null ? element == null : e.equals(element))
				return true;
		}

		return false;
	}

	/**
	 * Yields the first element of this list, if any.
	 * 
	 * @return the first element
	 * @throws NoSuchElementException if this list is empty
	 */
	public @View E first() {
		if (first == null)
			throw new NoSuchElementException();
		else
			return first.element;
	}

	/**
	 * Yields the last element of this list, if any.
	 * 
	 * @return the last element
	 * @throws NoSuchElementException if this list is empty
	 */
	public @View E last() {
		if (last == null)
			throw new NoSuchElementException();
		else
			return last.element;
	}

	/**
	 * Yields the element of this list at position {@code index}.
	 * 
	 * @param index the index of the element, between 0 (inclusive) and {@code size() - 1} (exclusive)
	 * @return the element at the given index
	 * @throws IndexOutOfBoundsException if the index is negative or equal or greater than
	 *                                   the size of this list
	 */
	public @View E get(int index) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException(String.valueOf(index));

		Node<E> cursor = first;
		for (int indexCopy = index; indexCopy > 0; indexCopy--)
			cursor = cursor.next;

		return cursor.element;
	}

	/**
	 * Yields the size of this list.
	 * 
	 * @return the size of this list
	 */
	public @View int size() {
		return size;
	}

	@Override
	public void forEach(Consumer<? super E> what) {
		for (Node<E> cursor = first; cursor != null; cursor = cursor.next)
			what.accept(cursor.element);
	}

	@Override @View
	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		for (Node<E> cursor = first; cursor != null; cursor = cursor.next) {
			if (cursor != first)
				sb.append(',');

			sb.append(cursor.element.toString());
		}

		sb.append(']');

		return sb.toString();
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private Node<E> cursor = first;

			@Override
			public boolean hasNext() {
				return cursor != null;
			}

			@Override
			public E next() {
				E result = cursor.element;
				cursor = cursor.next;

				return result;
			}
		};
	}

	/**
	 * Yields an ordered (first to last) stream of the elements of this list.
	 * 
	 * @return the stream
	 */
	public Stream<E> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * Yields an array containing the elements of this list, in their order in the list,
	 * using the provided generator function to allocate the returned array.
	 * 
	 * @param generator the array generator
	 * @return the array
	 * @throws ArrayStoreException if the runtime type of the array returned from the array generator
	 *                             is not a supertype of the runtime type of every element in this list
	 */
	public <A> A[] toArray(IntFunction<A[]> generator) {
		return stream().toArray(generator);
	}
}