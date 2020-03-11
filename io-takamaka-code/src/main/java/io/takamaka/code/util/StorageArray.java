package io.takamaka.code.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

/**
 * An array of (possibly {@code null}) storage values, that can be kept in storage.
 * By iterating on this object, one gets the values of the array, in increasing index
 * order, including {@code null}s.
 *
 * This code is derived from Sedgewick and Wayne's code for
 * red-black trees, with some adaptation. It implements an associative
 * map from indexes to values. The map can be kept in storage.
 * Values must have type allowed in storage.
 *
 * This class represents an ordered symbol table of generic index-value pairs.
 * It supports the usual <em>put</em> and <em>get</em> methods.
 * A symbol table implements the <em>associative array</em> abstraction:
 * when associating a value with an index that is already in the symbol table,
 * the convention is to replace the old value with the new value.
 * <p>
 * This implementation uses a left-leaning red-black BST.
 * The <em>put</em> and <em>get</em> operations each take
 * logarithmic time in the worst case, if the tree becomes unbalanced.
 * Construction takes constant time.
 * <p>
 * For additional documentation, see <a href="https://algs4.cs.princeton.edu/33balanced">Section 3.3</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 * @author Robert Sedgewick
 * @author Kevin Wayne
 * @param <V> the type of the values
 */

public class StorageArray<V> extends Storage implements Iterable<V> {
	private static final boolean RED   = true;
	private static final boolean BLACK = false;

	/**
	 * The root of the tree.
	 */
	private Node<V> root;

	/**
	 * The immutable size of the array.
	 */
	public final int length;

	/**
	 * A node of the binary search tree that implements the map.
	 */
	private static class Node<V> extends Storage {
		private int index;
		private V value; // possibly null
		private Node<V> left, right;
		private boolean color;

		private Node(int index, V value, boolean color, int size) {
			this.index = index;
			this.value = value;
			this.color = color;
		}

		@Override
		public int hashCode() { // to allow to store Nodes inside Java lists
			return 42;
		}
	}

	/**
	 * Builds an empty array of the given length.
	 * 
	 * @param length the length of the array
	 * @throws NegativeArraySizeException if {@code length} is negative
	 */
	public StorageArray(int length) {
		if (length < 0)
			throw new NegativeArraySizeException();

		this.length = length;
	}

	/**
	 * Builds an array of the given length, whose elements
	 * are all initialized to the given value.
	 * 
	 * @param length the length of the array
	 * @param initialValue the initial value of the array
	 * @throws NegativeArraySizeException if {@code length} is negative
	 */
	public StorageArray(int length, V initialValue) {
		this(length);

		IntStream.range(0, length).forEachOrdered(index -> set(index, initialValue));
	}

	/**
	 * Builds an array of the given length, whose elements
	 * are all initialized to the value provided by the given supplier.
	 * 
	 * @param length the length of the array
	 * @param supplier the supplier of the initial values of the array. It gets
	 *                 used repeatedly for each element to initialize
	 * @throws NegativeArraySizeException if {@code length} is negative
	 */
	public StorageArray(int length, Supplier<V> supplier) {
		this(length);

		IntStream.range(0, length).forEachOrdered(index -> set(index, supplier.get()));
	}

	/**
	 * Builds an array of the given length, whose elements
	 * are all initialized to the value provided by the given supplier.
	 * 
	 * @param length the length of the array
	 * @param supplier the supplier of the initial values of the array. It gets
	 *                 used repeatedly for each element to initialize:
	 *                 element at index <em>i</em> gets assigned
	 *                 {@code supplier.apply(i)}
	 * @throws NegativeArraySizeException if {@code length} is negative
	 */
	public StorageArray(int length, IntFunction<V> supplier) {
		this(length);

		IntStream.range(0, length).forEachOrdered(index -> set(index, supplier.apply(index)));
	}

	/**
	 * Determines if the given node is red.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is red
	 */
	private static <V> boolean isRed(Node<V> x) {
		return x != null && x.color == RED;
	}

	/**
	 * Determines if the given node is black.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is black
	 */
	private static <V> boolean isBlack(Node<V> x) {
		return x == null || x.color == BLACK;
	}

	private static int compareTo(int index1, int index2) {
		return index1 - index2;
	}

	/**
	 * Yields the value at the given index, if any. This operation runs in logarithmic time.
	 * 
	 * @param index the index
	 * @return the value at the given index if the index has been assigned to a value
	 *         and {@code null} otherwise
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	public @View V get(int index) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index);

		return get(root, index);
	}

	/**
	 * Yields the value associated with the given index in subtree rooted at x;
	 * 
	 * @param x the root of the subtree
	 * @param index the index
	 * @return the value. Yields {@code null} if the index is not found
	 */
	private static <V> V get(Node<V> x, int index) {
		while (x != null) {
			int cmp = compareTo(index, x.index);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return x.value;
		}
		return null;
	}

	/**
	 * Yields the value at the given index, if any. This operation runs in logarithmic time.
	 * 
	 * @param index the index
	 * @return the value at the given index if the index has been assigned to a value
	 *         and {@code _default} otherwise
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	public @View V getOrDefault(int index, V _default) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index);

		return getOrDefault(root, index, _default);
	}

	private static <V> V getOrDefault(Node<V> x, int index, V _default) {
		while (x != null) {
			int cmp = compareTo(index, x.index);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return x.value;
		}
		return _default;
	}

	/**
	 * Yields the value at the given index, if any. This operation runs in logarithmic time.
	 * 
	 * @param index the index
	 * @return the value at the given index if the index has been assigned to a value
	 *         and {@code _default.get()} otherwise
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	public V getOrDefault(int index, Supplier<V> _default) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index);

		return getOrDefault(root, index, _default);
	}

	// value associated with the given index in subtree rooted at x; uses supplier if no such index is found
	private static <V> V getOrDefault(Node<V> x, int index, Supplier<V> _default) {
		while (x != null) {
			int cmp = compareTo(index, x.index);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return x.value;
		}
		return _default.get();
	}

	/**
	 * Sets the value at the given index. This operation runs in logarithmic time.
	 *
	 * @param index the index
	 * @param value the value
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	public void set(int index, V value) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index);

		root = set(root, index, value);
		root.color = BLACK;
		// assert check();
	}

	// insert the index-value pair in the subtree rooted at h
	private static <V> Node<V> set(Node<V> h, int index, V value) { 
		if (h == null) return new Node<>(index, value, RED, 1);

		int cmp = compareTo(index, h.index);
		if      (cmp < 0) h.left  = set(h.left,  index, value); 
		else if (cmp > 0) h.right = set(h.right, index, value); 
		else              h.value = value;

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
		if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);

		return h;
	}

	// make a left-leaning link lean to the right
	private static <V> Node<V> rotateRight(Node<V> h) {
		// assert (h != null) && isRed(h.left);
		Node<V> x = h.left;
		h.left = x.right;
		x.right = h;
		x.color = h.color;
		h.color = RED;
		return x;
	}

	// make a right-leaning link lean to the left
	private static <V> Node<V> rotateLeft(Node<V> h) {
		// assert (h != null) && isRed(h.right);
		Node<V> x = h.right;
		h.right = x.left;
		x.left = h;
		x.color = h.color;
		h.color = RED;
		return x;
	}

	// flip the colors of a node and its two children
	private static <V> void flipColors(Node<V> h) {
		// h must have opposite color of its two children
		// assert (h != null) && (h.left != null) && (h.right != null);
		// assert (isBlack(h) &&  isRed(h.left) &&  isRed(h.right))
		//    || (isRed(h)  && isBlack(h.left) && isBlack(h.right));
		h.color = !h.color;
		h.left.color = !h.left.color;
		h.right.color = !h.right.color;
	}

	/**
	 * Replaces the old value {@code e} at {@code index} with {@code how.apply(e)}.
	 * If {@code index} was unmapped, it will be replaced with {@code how.apply(null)},
	 * which might well lead to a run-time exception.
	 *
	 * @param index the index whose value must be replaced
	 * @param how the replacement function
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	public void update(int index, UnaryOperator<V> how) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index);

		root = update(root, index, how);
		root.color = BLACK;
	}

	private static <V> Node<V> update(Node<V> h, int index, UnaryOperator<V> how) { 
		if (h == null) return new Node<>(index, how.apply(null), RED, 1);

		int cmp = compareTo(index, h.index);
		if      (cmp < 0) h.left  = update(h.left,  index, how); 
		else if (cmp > 0) h.right = update(h.right, index, how); 
		else              h.value = how.apply(h.value);

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
		if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);

		return h;
	}

	/**
	 * Replaces the old value {@code e} at {@code index} with {@code how.apply(e)}.
	 * If {@code index} was unmapped, it will be replaced with {@code how.apply(_default)}.
	 *
	 * @param index the index whose value must be replaced
	 * @param _default the default value
	 * @param how the replacement function
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	public void update(int index, V _default, UnaryOperator<V> how) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index);	

		root = update(root, index, _default, how);
		root.color = BLACK;
	}

	private static <V> Node<V> update(Node<V> h, int index, V _default, UnaryOperator<V> how) { 
		if (h == null) return new Node<>(index, how.apply(_default), RED, 1);

		int cmp = compareTo(index, h.index);
		if      (cmp < 0) h.left  = update(h.left, index, _default, how); 
		else if (cmp > 0) h.right = update(h.right, index, _default, how); 
		else if (h.value == null)
			h.value = how.apply(_default);
		else
			h.value = how.apply(h.value);

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
		if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);

		return h;
	}

	/**
	 * Replaces the old value {@code e} at {@code index} with {@code how.apply(e)}.
	 * If {@code index} was unmapped, it will be replaced with {@code how.apply(_default.get())}.
	 *
	 * @param index the index whose value must be replaced
	 * @param _default the supplier of the default value
	 * @param how the replacement function
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the bounds of the array
	 */
	public void update(int index, Supplier<V> _default, UnaryOperator<V> how) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index);	

		root = update(root, index, _default, how);
		root.color = BLACK;
	}

	private static <V> Node<V> update(Node<V> h, int index, Supplier<V> _default, UnaryOperator<V> how) { 
		if (h == null) return new Node<>(index, how.apply(_default.get()), RED, 1);

		int cmp = compareTo(index, h.index);
		if      (cmp < 0) h.left  = update(h.left, index, _default, how); 
		else if (cmp > 0) h.right = update(h.right, index, _default, how); 
		else if (h.value == null)
			h.value = how.apply(_default.get());
		else
			h.value = how.apply(h.value);

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
		if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);

		return h;
	}

	/**
	 * If the given index is unmapped or is mapped to {@code null}, map it to the given value.
	 * 
	 * @param index the index
	 * @param value the value
	 * @return the previous value at the given index. Yields {@code null} if {@code index} was previously unmapped
	 *         or was mapped to {@code null}
	 */
	public V setIfAbsent(int index, V value) {
		class SetIfAbsent {
			private V result;

			private Node<V> setIfAbsent(Node<V> h) {
				// not found: result remains null
				if (h == null)
					// not found
					return new Node<>(index, value, RED, 1);

				int cmp = compareTo(index, h.index);
				if      (cmp < 0) h.left  = setIfAbsent(h.left);
				else if (cmp > 0) h.right = setIfAbsent(h.right);
				else if (h.value == null) {
					// found but was bound to null: result remains null
					h.value = value;
					return h;
				}
				else {
					// found and was bound to a non-null value
					result = h.value;
					return h;
				}

				// fix-up any right-leaning links
				if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
				if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
				if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);

				return h;
			}
		}

		SetIfAbsent pia = new SetIfAbsent();
		root = pia.setIfAbsent(root);
		root.color = BLACK;

		return pia.result;
	}

	/**
	 * If the given index is unmapped or is mapped to {@code null}, map it to the value given by a supplier.
	 * 
	 * @param index the index
	 * @param supplier the supplier
	 * @return the previous value at the given index, if it was already mapped to a non-{@code null} value.
	 *         If the index was unmapped or was mapped to {@code null}, yields the new value
	 */
	public V computeIfAbsent(int index, Supplier<V> supplier) {
		class ComputeIfAbsent {
			private V result;

			private Node<V> computeIfAbsent(Node<V> h) { 
				if (h == null)
					// not found
					return new Node<>(index, result = supplier.get(), RED, 1);

				int cmp = compareTo(index, h.index);
				if      (cmp < 0) h.left  = computeIfAbsent(h.left);
				else if (cmp > 0) h.right = computeIfAbsent(h.right);
				else if (h.value == null) {
					// found but was bound to null
					result = h.value = supplier.get();
					return h;
				}
				else {
					// found and was bound to a non-null value
					result = h.value;
					return h;
				}

				// fix-up any right-leaning links
				if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
				if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
				if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);

				return h;
			}
		}

		ComputeIfAbsent cia = new ComputeIfAbsent();
		root = cia.computeIfAbsent(root);
		root.color = BLACK;

		return cia.result;
	}

	/**
	 * If the given index is unmapped or is mapped to {@code null}, map it to the value given by a supplier.
	 * 
	 * @param index the index
	 * @param supplier the supplier
	 * @return the previous value at the given index, if it was already mapped to a non-{@code null} value.
	 *         If the index was unmapped or was mapped to {@code null}, yields the new value
	 */
	public V computeIfAbsent(int index, IntFunction<V> supplier) {
		class ComputeIfAbsent {
			private V result;

			private Node<V> computeIfAbsent(Node<V> h) { 
				if (h == null)
					// not found
					return new Node<>(index, result = supplier.apply(index), RED, 1);

				int cmp = compareTo(index, h.index);
				if      (cmp < 0) h.left  = computeIfAbsent(h.left);
				else if (cmp > 0) h.right = computeIfAbsent(h.right);
				else if (h.value == null) {
					// found but was bound to null
					result = h.value = supplier.apply(index);
					return h;
				}
				else {
					// found and was bound to a non-null value
					result = h.value;
					return h;
				}

				// fix-up any right-leaning links
				if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
				if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
				if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);

				return h;
			}
		}

		ComputeIfAbsent cia = new ComputeIfAbsent();
		root = cia.computeIfAbsent(root);
		root.color = BLACK;

		return cia.result;
	}

	@Override
	public Iterator<V> iterator() {
		return new StorageArrayIterator<>(root, length);
	}

	private static class StorageArrayIterator<V> implements Iterator<V> {
		// the path under enumeration; it holds that the left children
		// have already been enumerated
		private List<Node<V>> stack = new ArrayList<>();
		private int nextKey;
		private final int length;

		private StorageArrayIterator(Node<V> root, int length) {
			this.length = length;

			// initially, the stack contains the leftmost path of the tree
			for (Node<V> cursor = root; cursor != null; cursor = cursor.left)
				stack.add(cursor);
		}

		@Override
		public boolean hasNext() {
			return nextKey < length;
		}

		@Override
		public V next() {
			// first check if we are in a hole of null values
			if (stack.isEmpty() || nextKey < stack.get(stack.size() - 1).index) {
				nextKey++;
				return null;
			}

			Node<V> topmost = stack.remove(stack.size() - 1);

			// we add the leftmost path of the right child of topmost
			for (Node<V> cursor = topmost.right; cursor != null; cursor = cursor.left)
				stack.add(cursor);

			nextKey++;
			return topmost.value;
		}
	}

	@Override
	public String toString() {
		return stream().map(Objects::toString).collect(Collectors.joining(",", "[", "]"));
	}

	/**
	 * Yields an ordered stream of the values in this array (including {@code null}s),
	 * in increasing order of index.
	 * 
	 * @return the stream
	 */
	public Stream<V> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * Yields an array containing the elements of this storage array, in their order,
	 * using the provided generator function to allocate the returned array.
	 * 
	 * @param generator the array generator
	 * @return the array
	 * @throws ArrayStoreException if the runtime type of the array returned from the array generator
	 *                             is not a supertype of the runtime type of every element in this storage array
	 */
	public <A> A[] toArray(IntFunction<A[]> generator) {
		return stream().toArray(generator);
	}
}