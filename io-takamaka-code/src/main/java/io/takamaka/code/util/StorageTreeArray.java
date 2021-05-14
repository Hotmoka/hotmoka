/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

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

import io.takamaka.code.lang.Exported;
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
 * <p>
 * This implementation uses a left-leaning red-black BST.
 * The <em>set</em> and <em>get</em> operations each take
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

public class StorageTreeArray<V> extends Storage implements StorageArray<V> {

	/**
	 * The root of the tree.
	 */
	private Node<V> root;

	/**
	 * The immutable size of the array.
	 */
	public final int length;

	/**
	 * Builds an empty array of the given length.
	 * 
	 * @param length the length of the array
	 * @throws NegativeArraySizeException if {@code length} is negative
	 */
	public StorageTreeArray(int length) {
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
	public StorageTreeArray(int length, V initialValue) {
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
	public StorageTreeArray(int length, Supplier<? extends V> supplier) {
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
	public StorageTreeArray(int length, IntFunction<? extends V> supplier) {
		this(length);
	
		IntStream.range(0, length).forEachOrdered(index -> set(index, supplier.apply(index)));
	}

	/**
	 * Yields a snapshot of the given array.
	 * 
	 * @param parent the array
	 */
	private StorageTreeArray(StorageTreeArray<V> parent) {
		this.root = parent.root;
		this.length = parent.length;
	}

	private void mkRootBlack() {
		if (isRed(root))
			root = Node.mkBlack(root.index, root.value, root.left, root.right);
	}

	/**
	 * A node of the binary search tree that implements the map.
	 */
	private abstract static class Node<V> extends Storage {
		protected final int index;
		protected final V value; // possibly null
		protected final Node<V> left, right;

		private Node(int index, V value, Node<V> left, Node<V> right) {
			this.index = index;
			this.value = value;
			this.left = left;
			this.right = right;
		}

		protected static <V> Node<V> mkBlack(int index, V value, Node<V> left, Node<V> right) {
			return new BlackNode<>(index, value, left, right);
		}

		protected static <V> Node<V> mkRed(int index, V value, Node<V> left, Node<V> right) {
			return new RedNode<>(index, value, left, right);
		}

		protected static <V> Node<V> mkRed(int index, V value) {
			return new RedNode<>(index, value, null, null);
		}

		@Override
		public int hashCode() { // unused, but needed to satisfy white-listing for addition of Nodes inside Java collections
			return 42;
		}

		protected abstract Node<V> setValue(V value);

		protected abstract Node<V> setLeft(Node<V> left);

		protected abstract Node<V> setRight(Node<V> right);

		protected abstract Node<V> rotateRight();

		protected abstract Node<V> rotateLeft();

		protected abstract Node<V> flipColors();

		protected abstract Node<V> flipColor();
	}

	private static class RedNode<V> extends Node<V> {

		private RedNode(int index, V value, Node<V> left, Node<V> right) {
			super(index, value, left, right);
		}

		@Override
		protected Node<V> flipColor() {
			return mkBlack(index, value, left, right);
		}

		@Override
		protected Node<V> rotateLeft() {
			final Node<V> x = right;
			Node<V> newThis = mkRed(index, value, left, x.left);
			return mkRed(x.index, x.value, newThis, x.right);
		}

		@Override
		protected Node<V> rotateRight() {
			final Node<V> x = left;
			Node<V> newThis = mkRed(index, value, x.right, right);
			return mkRed(x.index, x.value, x.left, newThis);
		}

		@Override
		protected Node<V> setValue(V value) {
			return mkRed(index, value, left, right);
		}

		@Override
		protected Node<V> setLeft(Node<V> left) {
			return mkRed(index, value, left, right);
		}

		@Override
		protected Node<V> setRight(Node<V> right) {
			return mkRed(index, value, left, right);
		}

		@Override
		protected Node<V> flipColors() {
			return mkBlack(index, value, left.flipColor(), right.flipColor());
		}
	}

	private static class BlackNode<V> extends Node<V> {

		private BlackNode(int index, V value, Node<V> left, Node<V> right) {
			super(index, value, left, right);
		}

		@Override
		protected Node<V> flipColor() {
			return mkRed(index, value, left, right);
		}

		@Override
		protected Node<V> rotateLeft() {
			final Node<V> x = right;
			Node<V> newThis = mkRed(index, value, left, x.left);
			return mkBlack(x.index, x.value, newThis, x.right);
		}

		@Override
		protected Node<V> rotateRight() {
			final Node<V> x = left;
			Node<V> newThis = mkRed(index, value, x.right, right);
			return mkBlack(x.index, x.value, x.left, newThis);
		}

		@Override
		protected Node<V> setValue(V value) {
			return mkBlack(index, value, left, right);
		}

		@Override
		protected Node<V> setLeft(Node<V> left) {
			return mkBlack(index, value, left, right);
		}

		@Override
		protected Node<V> setRight(Node<V> right) {
			return mkBlack(index, value, left, right);
		}

		@Override
		protected Node<V> flipColors() {
			return mkRed(index, value, left.flipColor(), right.flipColor());
		}
	}

	@Override
	public int length() {
		return length;
	}

	/**
	 * Determines if the given node is red.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is red
	 */
	private static <V> boolean isRed(Node<V> x) {
		return x instanceof RedNode<?>;
	}

	/**
	 * Determines if the given node is black.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is black
	 */
	private static <V> boolean isBlack(Node<V> x) {
		return x == null || x instanceof BlackNode<?>;
	}

	private static int compareTo(int index1, int index2) {
		return index1 - index2;
	}

	@Override
	public @View V get(int index) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index + " in get is outside bounds [0," + length + ")");

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

	@Override
	public @View V getOrDefault(int index, V _default) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index + " in getOrDefault is outside bounds [0," + length + ")");

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

	@Override
	public V getOrDefault(int index, Supplier<? extends V> _default) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index + " in getOrDefault is outside bounds [0," + length + ")");

		return getOrDefault(root, index, _default);
	}

	// value associated with the given index in subtree rooted at x; uses supplier if no such index is found
	private static <V> V getOrDefault(Node<V> x, int index, Supplier<? extends V> _default) {
		while (x != null) {
			int cmp = compareTo(index, x.index);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return x.value;
		}
		return _default.get();
	}

	@Override
	public void set(int index, V value) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index + " in set is outside bounds [0," + length + ")");

		root = set(root, index, value);
		mkRootBlack();
	}

	// insert the index-value pair in the subtree rooted at h
	private static <V> Node<V> set(Node<V> h, int index, V value) { 
		if (h == null) return Node.mkRed(index, value);

		int cmp = compareTo(index, h.index);
		if      (cmp < 0) h = h.setLeft(set(h.left,  index, value)); 
		else if (cmp > 0) h = h.setRight(set(h.right, index, value)); 
		else              h = h.setValue(value);

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = h.rotateLeft();
		if (isRed(h.left)  &&  isRed(h.left.left)) h = h.rotateRight();
		if (isRed(h.left)  &&  isRed(h.right))     h = h.flipColors();

		return h;
	}

	@Override
	public void update(int index, UnaryOperator<V> how) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index + " in update is outside bounds [0," + length + ")");

		root = update(root, index, how);
		mkRootBlack();
	}

	private static <V> Node<V> update(Node<V> h, int index, UnaryOperator<V> how) { 
		if (h == null) return Node.mkRed(index, how.apply(null));

		int cmp = compareTo(index, h.index);
		if      (cmp < 0) h = h.setLeft(update(h.left,  index, how)); 
		else if (cmp > 0) h = h.setRight(update(h.right, index, how)); 
		else              h = h.setValue(how.apply(h.value));

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = h.rotateLeft();
		if (isRed(h.left)  &&  isRed(h.left.left)) h = h.rotateRight();
		if (isRed(h.left)  &&  isRed(h.right))     h = h.flipColors();

		return h;
	}

	@Override
	public void update(int index, V _default, UnaryOperator<V> how) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index + " in update is outside bounds [0," + length + ")");

		root = update(root, index, _default, how);
		mkRootBlack();
	}

	private static <V> Node<V> update(Node<V> h, int index, V _default, UnaryOperator<V> how) { 
		if (h == null) return Node.mkRed(index, how.apply(_default));

		int cmp = compareTo(index, h.index);
		if      (cmp < 0) h = h.setLeft(update(h.left, index, _default, how)); 
		else if (cmp > 0) h = h.setRight(update(h.right, index, _default, how)); 
		else if (h.value == null)
			h = h.setValue(how.apply(_default));
		else
			h = h.setValue(how.apply(h.value));

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = h.rotateLeft();
		if (isRed(h.left)  &&  isRed(h.left.left)) h = h.rotateRight();
		if (isRed(h.left)  &&  isRed(h.right))     h = h.flipColors();

		return h;
	}

	@Override
	public void update(int index, Supplier<? extends V> _default, UnaryOperator<V> how) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index + " in update is outside bounds [0," + length + ")");

		root = update(root, index, _default, how);
		mkRootBlack();
	}

	private static <V> Node<V> update(Node<V> h, int index, Supplier<? extends V> _default, UnaryOperator<V> how) { 
		if (h == null) return Node.mkRed(index, how.apply(_default.get()));

		int cmp = compareTo(index, h.index);
		if      (cmp < 0) h = h.setLeft(update(h.left, index, _default, how)); 
		else if (cmp > 0) h = h.setRight(update(h.right, index, _default, how)); 
		else if (h.value == null)
			h = h.setValue(how.apply(_default.get()));
		else
			h = h.setValue(how.apply(h.value));

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = h.rotateLeft();
		if (isRed(h.left)  &&  isRed(h.left.left)) h = h.rotateRight();
		if (isRed(h.left)  &&  isRed(h.right))     h = h.flipColors();

		return h;
	}

	@Override
	public V setIfAbsent(int index, V value) {
		class SetIfAbsent {
			private V result;

			private Node<V> setIfAbsent(Node<V> h) {
				// not found: result remains null
				if (h == null)
					// not found
					return Node.mkRed(index, value);

				int cmp = compareTo(index, h.index);
				if      (cmp < 0) h = h.setLeft(setIfAbsent(h.left));
				else if (cmp > 0) h = h.setRight(setIfAbsent(h.right));
				else if (h.value == null)
					// found but was bound to null: result remains null
					return h.setValue(value);
				else {
					// found and was bound to a non-null value
					result = h.value;
					return h;
				}

				// fix-up any right-leaning links
				if (isRed(h.right) && isBlack(h.left))     h = h.rotateLeft();
				if (isRed(h.left)  &&  isRed(h.left.left)) h = h.rotateRight();
				if (isRed(h.left)  &&  isRed(h.right))     h = h.flipColors();

				return h;
			}
		}

		SetIfAbsent pia = new SetIfAbsent();
		root = pia.setIfAbsent(root);
		mkRootBlack();

		return pia.result;
	}

	@Override
	public V computeIfAbsent(int index, Supplier<? extends V> supplier) {
		class ComputeIfAbsent {
			private V result;

			private Node<V> computeIfAbsent(Node<V> h) { 
				if (h == null)
					// not found
					return Node.mkRed(index, result = supplier.get());

				int cmp = compareTo(index, h.index);
				if      (cmp < 0) h = h.setLeft(computeIfAbsent(h.left));
				else if (cmp > 0) h = h.setRight(computeIfAbsent(h.right));
				else if (h.value == null) {
					// found but was bound to null
					h = h.setValue(supplier.get());
					result = h.value;
					return h;
				}
				else {
					// found and was bound to a non-null value
					result = h.value;
					return h;
				}

				// fix-up any right-leaning links
				if (isRed(h.right) && isBlack(h.left))     h = h.rotateLeft();
				if (isRed(h.left)  &&  isRed(h.left.left)) h = h.rotateRight();
				if (isRed(h.left)  &&  isRed(h.right))     h = h.flipColors();

				return h;
			}
		}

		ComputeIfAbsent cia = new ComputeIfAbsent();
		root = cia.computeIfAbsent(root);
		mkRootBlack();

		return cia.result;
	}

	@Override
	public V computeIfAbsent(int index, IntFunction<? extends V> supplier) {
		class ComputeIfAbsent {
			private V result;

			private Node<V> computeIfAbsent(Node<V> h) { 
				if (h == null)
					// not found
					return Node.mkRed(index, result = supplier.apply(index));

				int cmp = compareTo(index, h.index);
				if      (cmp < 0) h = h.setLeft(computeIfAbsent(h.left));
				else if (cmp > 0) h = h.setRight(computeIfAbsent(h.right));
				else if (h.value == null) {
					// found but was bound to null
					h = h.setValue(supplier.apply(index));
					result = h.value;
					return h;
				}
				else {
					// found and was bound to a non-null value
					result = h.value;
					return h;
				}

				// fix-up any right-leaning links
				if (isRed(h.right) && isBlack(h.left))     h = h.rotateLeft();
				if (isRed(h.left)  &&  isRed(h.left.left)) h = h.rotateRight();
				if (isRed(h.left)  &&  isRed(h.right))     h = h.flipColors();

				return h;
			}
		}

		ComputeIfAbsent cia = new ComputeIfAbsent();
		root = cia.computeIfAbsent(root);
		mkRootBlack();

		return cia.result;
	}

	@Override
	public Iterator<V> iterator() {
		return new StorageArrayIterator<>(root, length);
	}

	private static class StorageArrayIterator<V> implements Iterator<V> {
		// the path under enumeration; it holds that the left children have already been enumerated
		private final List<Node<V>> stack = new ArrayList<>();
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
	public Stream<V> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	@Override
	public <A> A[] toArray(IntFunction<A[]> generator) {
		return stream().toArray(generator);
	}

	@Override
	public StorageArrayView<V> view() {

		/**
		 * A read-only view of a parent storage array. A view contains the same elements
		 * as the parent storage array, but does not include modification methods.
		 * Moreover, a view is exported, so that it can be safely divulged
		 * outside the store of a node. Calls to the view are simply forwarded to
		 * the parent array.
		 */

		@Exported
		class StorageArrayViewImpl extends Storage implements StorageArrayView<V> {

			@Override
			public Iterator<V> iterator() {
				return StorageTreeArray.this.iterator();
			}

			@Override
			public V get(int index) {
				return StorageTreeArray.this.get(index);
			}

			@Override
			public V getOrDefault(int index, V _default) {
				return StorageTreeArray.this.getOrDefault(index, _default);
			}

			@Override
			public V getOrDefault(int index, Supplier<? extends V> _default) {
				return StorageTreeArray.this.getOrDefault(index, _default);
			}

			@Override
			public Stream<V> stream() {
				return StorageTreeArray.this.stream();
			}

			@Override
			public <A> A[] toArray(IntFunction<A[]> generator) {
				return StorageTreeArray.this.toArray(generator);
			}

			@Override
			public String toString() {
				return StorageTreeArray.this.toString();
			}

			@Override
			public int length() {
				return StorageTreeArray.this.length();
			}

			@Override
			public StorageArrayView<V> snapshot() {
				return StorageTreeArray.this.snapshot();
			}
		}

		return new StorageArrayViewImpl();
	}

	@Override
	public StorageArrayView<V> snapshot() {
		return new StorageTreeArray<>(this).view();
	}

	@Override
	public String toString() {
		return stream().map(Objects::toString).collect(Collectors.joining(",", "[", "]"));
	}
}