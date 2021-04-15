package io.takamaka.code.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

/**
 * A sorted set of (non-{@code null}) storage values,
 * that can be kept in storage. By iterating on this object, one gets
 * the values in the set, in increasing order.
 *
 * This code is derived from Sedgewick and Wayne's code for
 * red-black trees, with some adaptation. It implements an associative
 * map from keys to a present/missing mark. The map can be kept in storage.
 * Values must have types allowed in storage. They are kept in
 * comparable order, if they implement {@link java.lang.Comparable}.
 * Otherwise, they must extend {@link io.takamaka.code.lang.Storage} and
 * are kept in storage reference order. This implementation does not call
 * {@code equals()} nor {@code hashCode()}.
 *
 * This class represents an ordered set of values.
 * It supports the usual <em>add</em>, <em>contains</em>,
 * <em>remove</em>, <em>size</em>, and <em>is-empty</em> methods.
 * It also provides ordered methods for finding the <em>minimum</em>,
 * <em>maximum</em>, <em>floor</em>, and <em>ceiling</em>.
 * <p>
 * The <em>add</em>, <em>contains</em>, <em>delete</em>, <em>minimum</em>,
 * <em>maximum</em>, <em>ceiling</em>, and <em>floor</em> operations each take
 * logarithmic time in the worst case, if the tree becomes unbalanced.
 * The <em>size</em>, and <em>is-empty</em> operations take constant time.
 * Construction takes constant time.
 * <p>
 * For additional documentation, see <a href="https://algs4.cs.princeton.edu/33balanced">Section 3.3</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 * @author Robert Sedgewick
 * @author Kevin Wayne
 * @param <V> the type of the values
 */

public class StorageTreeSet<V> extends Storage implements StorageSet<V> {

	/**
	 * The root of the tree.
	 */
	private Node<V> root;

	/**
	 * Builds an empty set.
	 */
	public StorageTreeSet() {}

	/**
	 * Creates a set initialized to the same elements as the given parent collection.
	 * 
	 * @param parent the parent collection
	 */
	public StorageTreeSet(Collection<? extends V> parent) {
		parent.forEach(this::add);
	}

	/**
	 * Yields a snapshot of the given set.
	 * 
	 * @param parent the map
	 */
	private StorageTreeSet(StorageTreeSet<V> parent) {
		this.root = parent.root;
	}

	private void mkRootBlack() {
		if (isRed(root))
			root = Node.mkBlack(root.value, root.size, root.left, root.right);
	}

	private void mkRootRed() {
		if (isBlack(root))
			root = Node.mkRed(root.value, root.size, root.left, root.right);
	}

	/**
	 * A node of the binary search tree that implements the set.
	 */
	private abstract static class Node<V> extends Storage {
		protected final V value; // never null
		protected final Node<V> left, right;

		/**
		 * Count of the subtree nodes.
		 */
		protected final int size;

		private Node(V value, int size, Node<V> left, Node<V> right) {
			this.value = value;
			this.size = size;
			this.left = left;
			this.right = right;
		}

		protected static <V> Node<V> mkBlack(V value, int size, Node<V> left, Node<V> right) {
			return new BlackNode<>(value, size, left, right);
		}

		protected static <V> Node<V> mkRed(V value, int size, Node<V> left, Node<V> right) {
			return new RedNode<>(value, size, left, right);
		}

		protected static <V> Node<V> mkRed(V value) {
			return new RedNode<>(value, 1, null, null);
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

		protected abstract Node<V> fixSize();

		protected abstract Node<V> flipColor();

		private Node<V> moveRedLeft() {
			Node<V> h = flipColors();
			return isRed(h.right.left) ? h.setRight(h.right.rotateRight()).rotateLeft().flipColors() : h;
		}

		private Node<V> moveRedRight() {
			Node<V> h = flipColors();
			return isRed(h.left.left) ? h.rotateRight().flipColors() : h;
		}

		// restore red-black tree invariant
		private Node<V> balance() {
			Node<V> h = this;
			if (isRed(right))                    h = h.rotateLeft();
			if (isRed(left) && isRed(left.left)) h = h.rotateRight();
			if (isRed(left) && isRed(right))     h = h.flipColors();

			return h.fixSize();
		}
	}

	private static class RedNode<V> extends Node<V> {

		private RedNode(V value, int size, Node<V> left, Node<V> right) {
			super(value, size, left, right);
		}

		@Override
		protected Node<V> fixSize() {
			return mkRed(value, size(left) + size(right) + 1, left, right);
		}

		@Override
		protected Node<V> flipColor() {
			return mkBlack(value, size, left, right);
		}

		@Override
		protected Node<V> rotateLeft() {
			final Node<V> x = right;
			Node<V> newThis = mkRed(value, size(x.left) + size(left) + 1, left, x.left);
			return mkRed(x.value, size, newThis, x.right);
		}

		@Override
		protected Node<V> rotateRight() {
			// assert isRed(left);
			final Node<V> x = left;
			Node<V> newThis = mkRed(value, size(x.right) + size(right) + 1, x.right, right);
			return mkRed(x.value, size, x.left, newThis);
		}

		@Override
		protected Node<V> setValue(V value) {
			return mkRed(value, size, left, right);
		}

		@Override
		protected Node<V> setLeft(Node<V> left) {
			return mkRed(value, size, left, right);
		}

		@Override
		protected Node<V> setRight(Node<V> right) {
			return mkRed(value, size, left, right);
		}

		@Override
		protected Node<V> flipColors() {
			return mkBlack(value, size, left.flipColor(), right.flipColor());
		}
	}

	private static class BlackNode<V> extends Node<V> {

		private BlackNode(V value, int size, Node<V> left, Node<V> right) {
			super(value, size, left, right);
		}

		@Override
		protected Node<V> fixSize() {
			return mkBlack(value, size(left) + size(right) + 1, left, right);
		}

		@Override
		protected Node<V> flipColor() {
			return mkRed(value, size, left, right);
		}

		@Override
		protected Node<V> rotateLeft() {
			final Node<V> x = right;
			Node<V> newThis = mkRed(value, size(x.left) + size(left) + 1, left, x.left);
			return mkBlack(x.value, size, newThis, x.right);
		}

		@Override
		protected Node<V> rotateRight() {
			final Node<V> x = left;
			Node<V> newThis = mkRed(value, size(x.right) + size(right) + 1, x.right, right);
			return mkBlack(x.value, size, x.left, newThis);
		}

		@Override
		protected Node<V> setValue(V value) {
			return mkBlack(value, size, left, right);
		}

		@Override
		protected Node<V> setLeft(Node<V> left) {
			return mkBlack(value, size, left, right);
		}

		@Override
		protected Node<V> setRight(Node<V> right) {
			return mkBlack(value, size, left, right);
		}

		@Override
		protected Node<V> flipColors() {
			return mkRed(value, size, left.flipColor(), right.flipColor());
		}
	}

	/**
	 * Determines if the given node is red.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is red
	 */
	private static <K,V> boolean isRed(Node<V> x) {
		return x instanceof RedNode<?>;
	}

	/**
	 * Determines if the given node is black.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is black
	 */
	private static <K,V> boolean isBlack(Node<V> x) {
		return x == null || x instanceof BlackNode<?>;
	}

	/**
	 * Yields the number of nodes in the subtree rooted at x.
	 * 
	 * @param x the root of the subtree
	 * @return the number of nodes. Yields 0 if {@code x} is {@code null}
	 */
	private static <K> int size(Node<K> x) {
		return x == null ? 0 : x.size;
	}

	@Override
	public @View int size() {
		return size(root);
	}

	@Override
	public @View boolean isEmpty() {
		return root == null;
	}

	@SuppressWarnings("unchecked")
	private static <K> int compareTo(K key1, K key2) {
		if (key1 instanceof Comparable<?>)
			return ((Comparable<K>) key1).compareTo(key2);
		else
			return ((Storage) key1).compareByStorageReference((Storage) key2);
	}

	/**
	 * Determines if the given subtree contains the given value.
	 * 
	 * @param x the root of the subtree
	 * @param value the value
	 * @return true if and only if that condition holds
	 */
	private static <V> boolean contains(Node<V> x, Object value) {
		while (x != null) {
			int cmp = compareTo(value, x.value);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return true;
		}

		return false;
	}

	@Override
	public @View boolean contains(Object value) {
		if (value == null) throw new IllegalArgumentException("value is null");
		return contains(root, value);
	}

	@Override
	public void add(V value) {
		if (value == null) throw new IllegalArgumentException("value is null");
		root = put(root, value);
		mkRootBlack();
	}

	// insert the value in the subtree rooted at h
	private static <V> Node<V> put(Node<V> h, V value) { 
		if (h == null) return Node.mkRed(value);

		int cmp = compareTo(value, h.value);
		if      (cmp < 0) h = h.setLeft(put(h.left, value)); 
		else if (cmp > 0) h = h.setRight(put(h.right, value));
		else              h = h.setValue(value);

		// fix-up any right-leaning links
		if (isRed(h.right) &&  isBlack(h.left))    h = h.rotateLeft();
		if (isRed(h.left)  &&  isRed(h.left.left)) h = h.rotateRight();
		if (isRed(h.left)  &&  isRed(h.right))     h = h.flipColors();
		
		return h.fixSize();
	}

	@Override
	public void removeMin() {
		if (isEmpty()) throw new NoSuchElementException();

		if (isBlack(root.left) && isBlack(root.right))
			mkRootRed();

		root = removeMin(root);
		if (!isEmpty()) mkRootBlack();
	}

	// removes the minimum value in the tree rooted at h
	private static <V> Node<V> removeMin(Node<V> h) {
		if (h.left == null)
			return null;

		if (isBlack(h.left) && isBlack(h.left.left))
			h = h.moveRedLeft();

		return h.setLeft(removeMin(h.left)).balance();
	}

	@Override
	public void removeMax() {
		if (isEmpty()) throw new NoSuchElementException();

		if (isBlack(root.left) && isBlack(root.right))
			mkRootRed();

		root = removeMax(root);
		if (!isEmpty()) mkRootBlack();
	}

	// delete the maximum value in the tree rooted at h
	private static <V> Node<V> removeMax(Node<V> h) { 
		if (isRed(h.left))
			h = h.rotateRight();

		if (h.right == null)
			return null;

		if (isBlack(h.right) && isBlack(h.right.left))
			h = h.moveRedRight();

		return h.setRight(removeMax(h.right)).balance();
	}

	@Override
	public void remove(Object value) { 
		if (value == null) throw new IllegalArgumentException("value is null");
		if (contains(value)) {
			if (isBlack(root.left) && isBlack(root.right))
				mkRootRed();

			root = remove(root, value);
			if (!isEmpty()) mkRootBlack();
		}
	}

	// delete the given value from the tree rooted at h
	private static <V> Node<V> remove(Node<V> h, Object value) {
		if (compareTo(value, h.value) < 0)  {
			if (isBlack(h.left) && isBlack(h.left.left))
				h = h.moveRedLeft();

			h = h.setLeft(remove(h.left, value));
		}
		else {
			if (isRed(h.left))
				h = h.rotateRight();
			if (compareTo(value, h.value) == 0 && (h.right == null))
				return null;
			if (isBlack(h.right) && isBlack(h.right.left))
				h = h.moveRedRight();
			if (compareTo(value, h.value) == 0) {
				Node<V> x = min(h.right);
				if (isRed(h))
					h = Node.mkRed(x.value, h.size, h.left, removeMin(h.right));
				else
					h = Node.mkBlack(x.value, h.size, h.left, removeMin(h.right));
			}
			else
				h = h.setRight(remove(h.right, value));
		}
		return h.balance();
	}

	@Override
	public @View V min() {
		if (isEmpty()) throw new NoSuchElementException("call to min() on an empty set");
		return min(root).value;
	} 

	// the smallest value in subtree rooted at x
	private static <V> Node<V> min(Node<V> x) { 
		if (x.left == null) return x; 
		else                return min(x.left); 
	} 

	@Override
	public @View V max() {
		if (isEmpty()) throw new NoSuchElementException("call to max() on an empty set");
		return max(root).value;
	} 

	// the largest value in the subtree rooted at x
	private static <V> Node<V> max(Node<V> x) { 
		if (x.right == null) return x; 
		else                 return max(x.right); 
	}

	@Override
	public @View V floorKey(Object value) {
		if (value == null) throw new IllegalArgumentException("value is null");
		if (isEmpty()) throw new NoSuchElementException();
		Node<V> x = floorKey(root, value);
		if (x == null) throw new NoSuchElementException();
		else           return x.value;
	}    

	// the largest value in the subtree rooted at x less than or equal to the given value
	private static <V> Node<V> floorKey(Node<V> x, Object value) {
		if (x == null) return null;
		int cmp = compareTo(value, x.value);
		if (cmp == 0) return x;
		if (cmp < 0)  return floorKey(x.left, value);
		Node<V> t = floorKey(x.right, value);
		if (t != null) return t; 
		else           return x;
	}

	@Override
	public @View V ceilingKey(Object value) {
		if (value == null) throw new IllegalArgumentException("value is null");
		if (isEmpty()) throw new NoSuchElementException();
		Node<V> x = ceilingKey(root, value);
		if (x == null) throw new NoSuchElementException();
		else           return x.value;  
	}

	// the smallest value in the subtree rooted at x greater than or equal to the given value
	private static <V> Node<V> ceilingKey(Node<V> x, Object value) {  
		if (x == null) return null;
		int cmp = compareTo(value, x.value);
		if (cmp == 0) return x;
		if (cmp > 0)  return ceilingKey(x.right, value);
		Node<V> t = ceilingKey(x.left, value);
		if (t != null) return t; 
		else           return x;
	}

	@Override
	public @View V select(int k) {
		if (k < 0 || k >= size()) throw new IllegalArgumentException("argument to select() is invalid: " + k);
		return select(root, k).value;
	}

	// yield the value of rank k in the subtree rooted at x
	private static <V> Node<V> select(Node<V> x, int k) {
		int t = size(x.left); 
		if      (t > k) return select(x.left,  k); 
		else if (t < k) return select(x.right, k - t - 1); 
		else            return x; 
	} 

	@Override
	public @View int rank(Object value) {
		if (value == null) throw new IllegalArgumentException("value is null");
		return rank(value, root);
	} 

	// number of values less than value in the subtree rooted at x
	private static <V> int rank(Object value, Node<V> x) {
		if (x == null) return 0; 
		int cmp = compareTo(value, x.value); 
		if      (cmp < 0) return rank(value, x.left); 
		else if (cmp > 0) return 1 + size(x.left) + rank(value, x.right); 
		else              return size(x.left); 
	} 

	@Override
	public Iterator<V> iterator() {
		return new StorageSetIterator<>(root);
	}

	private static class StorageSetIterator<V> implements Iterator<V> {
		// the path under enumeration; it is always true that the left children
		// have already been enumerated
		private final List<Node<V>> stack = new ArrayList<>();

		private StorageSetIterator(Node<V> root) {
			// initially, the stack contains the leftmost path of the tree
			for (Node<V> cursor = root; cursor != null; cursor = cursor.left)
				stack.add(cursor);
		}

		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		@Override
		public V next() {
			Node<V> topmost = stack.remove(stack.size() - 1);

			// we add the leftmost path of the right child of topmost
			for (Node<V> cursor = topmost.right; cursor != null; cursor = cursor.left)
				stack.add(cursor);

			return topmost.value;
		}
	}

	@Override
	public Stream<V> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	@Override
	public String toString() {
		return stream().map(Objects::toString).collect(Collectors.joining(",", "[", "]"));
	}

	@Override
	public StorageSetView<V> view() {

		/**
		 * A read-only view of a parent storage set. A view contains the same elements
		 * as the parent storage set, but does not include modification methods.
		 * Moreover, a view is exported, so that it can be safely divulged
		 * outside the store of a node. Calls to the view are simply forwarded to
		 * the parent set.
		 */

		@Exported
		class StorageSetViewImpl extends Storage implements StorageSetView<V> {

			@Override
			public @View int size() {
				return StorageTreeSet.this.size();
			}

			@Override
			public @View boolean isEmpty() {
				return StorageTreeSet.this.isEmpty();
			}

			@Override
			public @View boolean contains(Object value) {
				return StorageTreeSet.this.contains(value);
			}

			@Override
			public @View V min() {
				return StorageTreeSet.this.min();
			} 

			@Override
			public @View V max() {
				return StorageTreeSet.this.max();
			} 

			@Override
			public @View V floorKey(Object value) {
				return StorageTreeSet.this.floorKey(value);
			}    

			@Override
			public @View V ceilingKey(Object value) {
				return StorageTreeSet.this.ceilingKey(value);
			}

			@Override
			public @View V select(int k) {
				return StorageTreeSet.this.select(k);
			}

			@Override
			public @View int rank(Object value) {
				return StorageTreeSet.this.rank(value);
			} 

			@Override
			public String toString() {
				return StorageTreeSet.this.toString();
			}

			@Override
			public Iterator<V> iterator() {
				return StorageTreeSet.this.iterator();
			}

			@Override
			public Stream<V> stream() {
				return StorageTreeSet.this.stream();
			}

			@Override
			public StorageSetView<V> snapshot() {
				return StorageTreeSet.this.snapshot();
			}
		}

		return new StorageSetViewImpl();
	}

	@Override
	public StorageSetView<V> snapshot() {
		return new StorageTreeSet<>(this).view();
	}
}