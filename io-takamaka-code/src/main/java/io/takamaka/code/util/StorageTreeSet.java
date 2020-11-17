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

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.views.StorageSetView;

/**
 * A sorted set of (non-{@code null}) storage values,
 * that can be kept in storage. By iterating on this object, one gets
 * the values in the set, in increasing order.
 *
 * This code is derived from Sedgewick and Wayne's code for
 * red-black trees, with some adaptation. It implements an associative
 * map from keys to present/missing. The map can be kept in storage.
 * Values must have types allowed in storage. They are kept in
 * comparable order, if they implement {@link java.lang.Comparable}.
 * Otherwise, they must extend {@link io.takamaka.code.lang.Storage} and
 * are kept in storage reference order.
 *
 * This class represents an ordered set of values.
 * It supports the usual <em>add</em>, <em>contains</em>,
 * <em>remove</em>, <em>size</em>, and <em>is-empty</em> methods.
 * It also provides ordered methods for finding the <em>minimum</em>,
 * <em>maximum</em>, <em>floor</em>, and <em>ceiling</em>.
 * <p>
 * This implementation uses a left-leaning red-black BST. It requires that
 * the key type is a storage class or implements the {@code Comparable} interface
 * and in such a case calls the
 * {@code compareTo()} method to compare two keys. It does not call
 * {@code equals()} nor {@code hashCode()}.
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

public class StorageTreeSet<V> extends Storage implements ModifiableStorageSet<V> {
	private static final boolean RED   = true;
	private static final boolean BLACK = false;

	/**
	 * The root of the tree.
	 */
	private Node<V> root;

	/**
	 * A node of the binary search tree that implements the set.
	 */
	private static class Node<K> extends Storage {
		private K value; // always non-null
		private Node<K> left, right;
		private boolean color;

		/**
		 * Count of the subtree nodes.
		 */
		private int size;

		private Node(K key, boolean color, int size) {
			this.value = key;
			this.color = color;
			this.size = size;
		}

		@Override
		public int hashCode() {
			return 47;
		}
	}

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
	 * Determines if the given node is red.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is red
	 */
	private static <K> boolean isRed(Node<K> x) {
		return x != null && x.color == RED;
	}

	/**
	 * Determines if the given node is black.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is black
	 */
	private static <K> boolean isBlack(Node<K> x) {
		return x == null || x.color == BLACK;
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
		root.color = BLACK;
	}

	// insert the value in the subtree rooted at h
	private static <V> Node<V> put(Node<V> h, V value) { 
		if (h == null) return new Node<>(value, RED, 1);

		int cmp = compareTo(value, h.value);
		if      (cmp < 0) h.left  = put(h.left,  value); 
		else if (cmp > 0) h.right = put(h.right, value); 

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
		if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);
		h.size = size(h.left) + size(h.right) + 1;

		return h;
	}

	@Override
	public void removeMin() {
		if (isEmpty()) throw new NoSuchElementException();

		// if both children of root are black, set root to red
		if (isBlack(root.left) && isBlack(root.right))
			root.color = RED;

		root = removeMin(root);
		if (!isEmpty()) root.color = BLACK;
	}

	// removes the minimum value in the tree rooted at h
	private static <V> Node<V> removeMin(Node<V> h) { 
		if (h.left == null)
			return null;

		if (isBlack(h.left) && isBlack(h.left.left))
			h = moveRedLeft(h);

		h.left = removeMin(h.left);
		return balance(h);
	}

	@Override
	public void removeMax() {
		if (isEmpty()) throw new NoSuchElementException();

		// if both children of root are black, set root to red
		if (isBlack(root.left) && isBlack(root.right))
			root.color = RED;

		root = removeMax(root);
		if (!isEmpty()) root.color = BLACK;
	}

	// delete the maximum value in the tree rooted at h
	private static <V> Node<V> removeMax(Node<V> h) { 
		if (isRed(h.left))
			h = rotateRight(h);

		if (h.right == null)
			return null;

		if (isBlack(h.right) && isBlack(h.right.left))
			h = moveRedRight(h);

		h.right = removeMax(h.right);

		return balance(h);
	}

	@Override
	public void remove(Object value) { 
		if (value == null) throw new IllegalArgumentException("value is null");
		if (contains(value)) {
			// if both children of root are black, set root to red
			if (isBlack(root.left) && isBlack(root.right))
				root.color = RED;

			root = remove(root, value);
			if (!isEmpty()) root.color = BLACK;
		}
	}

	// delete the given value from the tree rooted at h
	private static <V> Node<V> remove(Node<V> h, Object value) { 
		if (compareTo(value, h.value) < 0)  {
			if (isBlack(h.left) && isBlack(h.left.left))
				h = moveRedLeft(h);
			h.left = remove(h.left, value);
		}
		else {
			if (isRed(h.left))
				h = rotateRight(h);
			if (compareTo(value, h.value) == 0 && (h.right == null))
				return null;
			if (isBlack(h.right) && isBlack(h.right.left))
				h = moveRedRight(h);
			if (compareTo(value, h.value) == 0) {
				Node<V> x = min(h.right);
				h.value = x.value;
				h.right = removeMin(h.right);
			}
			else h.right = remove(h.right, value);
		}
		return balance(h);
	}

	// make a left-leaning link lean to the right
	private static <V> Node<V> rotateRight(Node<V> h) {
		Node<V> x = h.left;
		h.left = x.right;
		x.right = h;
		x.color = h.color;
		h.color = RED;
		x.size = h.size;
		h.size = size(h.left) + size(h.right) + 1;
		return x;
	}

	// make a right-leaning link lean to the left
	private static <V> Node<V> rotateLeft(Node<V> h) {
		Node<V> x = h.right;
		h.right = x.left;
		x.left = h;
		x.color = h.color;
		h.color = RED;
		x.size = h.size;
		h.size = size(h.left) + size(h.right) + 1;
		return x;
	}

	// flip the colors of a node and its two children
	private static <V> void flipColors(Node<V> h) {
		// h must have opposite color of its two children
		h.color = !h.color;
		h.left.color = !h.left.color;
		h.right.color = !h.right.color;
	}

	// Assuming that h is red and both h.left and h.left.left
	// are black, make h.left or one of its children red
	private static <V> Node<V> moveRedLeft(Node<V> h) {
		flipColors(h);
		if (isRed(h.right.left)) { 
			h.right = rotateRight(h.right);
			flipColors(h = rotateLeft(h));
		}
		return h;
	}

	// Assuming that h is red and both h.right and h.right.left
	// are black, make h.right or one of its children red
	private static <V> Node<V> moveRedRight(Node<V> h) {
		flipColors(h);
		if (isRed(h.left.left))
			flipColors(h = rotateRight(h));

		return h;
	}

	// restore red-black tree invariant
	private static <K> Node<K> balance(Node<K> h) {
		if (isRed(h.right))                      h = rotateLeft(h);
		if (isRed(h.left) && isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left) && isRed(h.right))     flipColors(h);

		h.size = size(h.left) + size(h.right) + 1;
		return h;
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
		private List<Node<V>> stack = new ArrayList<>();

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
	public StorageSet<V> view() {
		return new StorageSetView<V>(this);
	}

	@Override
	public StorageSet<V> snapshot() {
		StorageTreeSet<V> copy = new StorageTreeSet<>();
		stream().forEachOrdered(copy::add);
		return copy.view();
	}
}