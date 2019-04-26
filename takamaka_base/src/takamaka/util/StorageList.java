package takamaka.util;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import takamaka.lang.Storage;

/**
 * A list of elements.
 *
 * @param <E> the type of the elements
 */
public class StorageList<E> extends Storage {
	private Node<E> first;
	private Node<E> last;
	private int size;

	private static class Node<E> extends Storage {
		private final E element;
		private Node<E> next;

		private Node(E element) {
			this.element = element;
		}

		private Node(E element, Node<E> next) {
			this.element = element;
			this.next = next;
		}
	}

	public void addFirst(E element) {
		if (first == null)
			first = last = new Node<E>(element);
		else
			first = new Node<E>(element, first);

		size++;
	}

	public void addLast(E element) {
		if (last == null)
			first = last = new Node<E>(element);
		else
			last = last.next = new Node<E>(element);

		size++;
	}

	public void add(E element) {
		addLast(element);
	}

	public void clear() {
		first = last = null;
		size = 0;
	}

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

	public E first() {
		if (first == null)
			throw new NoSuchElementException();
		else
			return first.element;
	}

	public E last() {
		if (last == null)
			throw new NoSuchElementException();
		else
			return last.element;
	}

	public E elementAt(int index) {
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

	public int size() {
		return size;
	}

	public void forEach(Consumer<E> what) {
		for (Node<E> cursor = first; cursor != null; cursor = cursor.next)
			what.accept(cursor.element);
	}

	@Override
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
}