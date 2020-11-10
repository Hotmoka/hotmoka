package io.takamaka.code.util.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.ModifiableStorageList;

/**
 * A list of elements that can be kept in storage. It is possible to
 * add and access elements at both sides of the list. This list can hold
 * {@code null} elements.
 *
 * @param <E> the type of the elements. This type must be allowed in storage
 */
public class ModifiableStorageListImpl<E> extends Storage implements ModifiableStorageList<E> {

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
	 * Creates an empty list.
	 */
	public ModifiableStorageListImpl() {
	}

	/**
	 * Creates a list initialized to the same elements as the given parent collection.
	 * 
	 * @param parent the parent collection
	 */
	public ModifiableStorageListImpl(Collection<? extends E> parent) {
		parent.forEach(this::add);
	}

	@Override
	public void addFirst(E element) {
		if (first == null)
			first = last = new Node<>(element);
		else
			first = new Node<>(element, first);

		size++;
	}

	@Override
	public void addLast(E element) {
		if (last == null)
			first = last = new Node<>(element);
		else
			last = last.next = new Node<>(element);

		size++;
	}

	@Override
	public void add(E element) {
		addLast(element);
	}

	@Override
	public void clear() {
		first = last = null;
		size = 0;
	}

	@Override
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

	@Override
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

	@Override
	public @View boolean contains(Object e) {
		for (Node<E> cursor = first; cursor != null; cursor = cursor.next) {
			E element = cursor.element;
			if (e == null ? element == null : e.equals(element))
				return true;
		}

		return false;
	}

	@Override
	public @View E first() {
		if (first == null)
			throw new NoSuchElementException();
		else
			return first.element;
	}

	@Override
	public @View E last() {
		if (last == null)
			throw new NoSuchElementException();
		else
			return last.element;
	}

	@Override
	public @View E get(int index) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException(String.valueOf(index));

		Node<E> cursor = first;
		for (int indexCopy = index; indexCopy > 0; indexCopy--)
			cursor = cursor.next;

		return cursor.element;
	}

	@Override
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

	@Override
	public Stream<E> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	@Override
	public <A> A[] toArray(IntFunction<A[]> generator) {
		return stream().toArray(generator);
	}
}