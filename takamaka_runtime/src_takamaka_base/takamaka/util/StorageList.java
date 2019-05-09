package takamaka.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import takamaka.lang.Storage;
import takamaka.lang.View;

/**
 * A list of elements that can be kept in storage. It is possible to
 * add and access elements at both sides of the list.
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
	 * @param element the element
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
	 * @param element the element
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
	 * This is synonym of {@link takamaka.util.StorageList#addFirst(Object)}.
	 * 
	 * @param element the element
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
	 * @param index the index of the element, starting at 0
	 * @return the element at the given element
	 * @throws IndexOutOfBoundsException if the index is negative or equal or greater than
	 *                                   the size of this list
	 */
	public @View E elementAt(int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException(String.valueOf(index));

		Node<E> cursor = first;
		int indexCopy = index;
		while (indexCopy-- > 0) {
			if (cursor == null)
				throw new IndexOutOfBoundsException(String.valueOf(index));

			cursor = cursor.next;
		}

		return cursor.element;
	}

	/**
	 * The size of this list.
	 * 
	 * @return the size
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
	 * Yields an ordered stream of the elements of this list.
	 * 
	 * @return the stream
	 */
	public Stream<E> stream() {
		return StreamSupport.stream(spliterator(), false);
	}
}