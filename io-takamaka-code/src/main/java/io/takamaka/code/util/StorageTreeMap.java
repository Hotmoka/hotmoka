package io.takamaka.code.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

/**
 * A map from storage keys to (possibly {@code null}) storage values,
 * that can be kept in storage. By iterating on this object, one gets
 * the key/value pairs of the map, in increasing key order.
 *
 * This code is derived from Sedgewick and Wayne's code for
 * red-black trees, with some adaptation. It implements an associative
 * map from keys to values. The map can be kept in storage. Keys
 * and values must have types allowed in storage. Keys are kept in
 * comparable order, if they implement {@link java.lang.Comparable}.
 * Otherwise, they must extend {@link io.takamaka.code.lang.Storage} and
 * are kept in storage reference order.
 *
 * This class represents an ordered symbol table of generic key-value pairs.
 * It supports the usual <em>put</em>, <em>get</em>, <em>contains</em>,
 * <em>remove</em>, <em>size</em>, and <em>is-empty</em> methods.
 * It also provides ordered methods for finding the <em>minimum</em>,
 * <em>maximum</em>, <em>floor</em>, and <em>ceiling</em>.
 * A symbol table implements the <em>associative array</em> abstraction:
 * when associating a value with a key that is already in the symbol table,
 * the convention is to replace the old value with the new value.
 * <p>
 * This implementation uses a left-leaning red-black BST. It requires that
 * the key type is a storage class or implements the {@code Comparable} interface
 * and in such a case calls the
 * {@code compareTo()} method to compare two keys. It does not call neither
 * {@code equals()} nor {@code hashCode()}.
 * The <em>put</em>, <em>contains</em>, <em>delete</em>, <em>minimum</em>,
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
 * @param <K> the type of the keys
 * @param <V> the type of the values
 */

public class StorageTreeMap<K,V> extends Storage implements StorageMap<K,V> {

	/**
	 * The root of the tree.
	 */
	private Node<K,V> root;

	/**
	 * Builds an empty map.
	 */
	public StorageTreeMap() {}

	/**
	 * Creates a map initialized to the same binings as the given parent map.
	 * 
	 * @param parent the parent map
	 */
	public StorageTreeMap(Map<? extends K, ? extends V> parent) {
		parent.forEach(this::put);
	}

	/**
	 * Yields a snapshot of the given map.
	 * 
	 * @param parent the map
	 */
	private StorageTreeMap(StorageTreeMap<K,V> parent) {
		this.root = parent.root;
	}

	private void mkRootBlack() {
		if (isRed(root))
			root = Node.mkBlack(root.key, root.value, root.size, root.left, root.right);
	}

	private void mkRootRed() {
		if (isBlack(root))
			root = Node.mkRed(root.key, root.value, root.size, root.left, root.right);
	}

	/**
	 * A node of the binary search tree that implements the map.
	 */
	private abstract static class Node<K,V> extends Storage implements Entry<K,V> {
		protected final K key; // always non-null
		protected final V value; // possibly null
		protected final Node<K,V> left, right;

		/**
		 * Count of the subtree nodes.
		 */
		protected final int size;

		private Node(K key, V value, int size, Node<K,V> left, Node<K,V> right) {
			this.key = key;
			this.value = value;
			this.size = size;
			this.left = left;
			this.right = right;
		}

		protected static <K,V> Node<K,V> mkBlack(K key, V value, int size, Node<K,V> left, Node<K,V> right) {
			return new BlackNode<>(key, value, size, left, right);
		}

		protected static <K,V> Node<K,V> mkRed(K key, V value, int size, Node<K,V> left, Node<K,V> right) {
			return new RedNode<>(key, value, size, left, right);
		}

		protected static <K,V> Node<K,V> mkRed(K key, V value, int size) {
			return new RedNode<>(key, value, size, null, null);
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public int hashCode() { // unused, but needed to satisfy white-listing for addition of Nodes inside Java collections
			return 42;
		}

		protected abstract Node<K,V> setValue(V value);

		protected abstract Node<K,V> setLeft(Node<K,V> left);

		protected abstract Node<K,V> setRight(Node<K,V> right);

		protected abstract Node<K,V> rotateRight();

		protected abstract Node<K,V> rotateLeft();

		protected abstract Node<K,V> flipColors();

		protected abstract Node<K,V> fixSize();

		protected abstract Node<K,V> flipColor();

		private Node<K,V> moveRedLeft() {
			// assert isRed(this) && isBlack(left) && isBlack(left.left);
			Node<K,V> h = flipColors();
			return isRed(h.right.left) ? h.setRight(h.right.rotateRight()).rotateLeft().flipColors() : h;
		}

		private Node<K,V> moveRedRight() {
			// assert isRed(this) && isBlack(right) && isBlack(right.left);
			Node<K,V> h = flipColors();
			return isRed(h.left.left) ? h.rotateRight().flipColors() : h;
		}

		// restore red-black tree invariant
		private Node<K,V> balance() {
			Node<K,V> h = this;
			if (isRed(right))                    h = h.rotateLeft();
			if (isRed(left) && isRed(left.left)) h = h.rotateRight();
			if (isRed(left) && isRed(right))     h = h.flipColors();

			return h.fixSize();
		}
	}

	private static class RedNode<K,V> extends Node<K,V> {

		private RedNode(K key, V value, int size, Node<K,V> left, Node<K,V> right) {
			super(key, value, size, left, right);
		}

		@Override
		protected Node<K,V> fixSize() {
			return mkRed(key, value, size(left) + size(right) + 1, left, right);
		}

		@Override
		protected Node<K, V> flipColor() {
			return mkBlack(key, value, size, left, right);
		}

		@Override
		protected Node<K, V> rotateLeft() {
			final Node<K,V> x = right;
			Node<K,V> newThis = mkRed(key, value, size(x.left) + size(left) + 1, left, x.left);
			return mkRed(x.key, x.value, size, newThis, x.right);
		}

		@Override
		protected Node<K, V> rotateRight() {
			// assert isRed(left);
			final Node<K,V> x = left;
			Node<K,V> newThis = mkRed(key, value, size(x.right) + size(right) + 1, x.right, right);
			return mkRed(x.key, x.value, size, x.left, newThis);
		}

		@Override
		protected Node<K, V> setValue(V value) {
			return mkRed(key, value, size, left, right);
		}

		@Override
		protected Node<K, V> setLeft(Node<K, V> left) {
			return mkRed(key, value, size, left, right);
		}

		@Override
		protected Node<K, V> setRight(Node<K, V> right) {
			return mkRed(key, value, size, left, right);
		}

		@Override
		protected Node<K,V> flipColors() {
			// h must have opposite color of its two children
			// assert (h != null) && (h.left != null) && (h.right != null);
			// assert (isBlack(h) &&  isRed(h.left) &&  isRed(h.right))
			//    || (isRed(h)  && isBlack(h.left) && isBlack(h.right));
			return mkBlack(key, value, size, left.flipColor(), right.flipColor());
		}
	}

	private static class BlackNode<K,V> extends Node<K,V> {

		private BlackNode(K key, V value, int size, Node<K,V> left, Node<K,V> right) {
			super(key, value, size, left, right);
		}

		@Override
		protected Node<K,V> fixSize() {
			return mkBlack(key, value, size(left) + size(right) + 1, left, right);
		}

		@Override
		protected Node<K, V> flipColor() {
			return mkRed(key, value, size, left, right);
		}

		@Override
		protected Node<K, V> rotateLeft() {
			final Node<K,V> x = right;
			Node<K,V> newThis = mkRed(key, value, size(x.left) + size(left) + 1, left, x.left);
			return mkBlack(x.key, x.value, size, newThis, x.right);
		}

		@Override
		protected Node<K, V> rotateRight() {
			// assert isRed(left);
			final Node<K,V> x = left;
			Node<K,V> newThis = mkRed(key, value, size(x.right) + size(right) + 1, x.right, right);
			return mkBlack(x.key, x.value, size, x.left, newThis);
		}

		@Override
		protected Node<K, V> setValue(V value) {
			return mkBlack(key, value, size, left, right);
		}

		@Override
		protected Node<K, V> setLeft(Node<K, V> left) {
			return mkBlack(key, value, size, left, right);
		}

		@Override
		protected Node<K, V> setRight(Node<K, V> right) {
			return mkBlack(key, value, size, left, right);
		}

		@Override
		protected Node<K,V> flipColors() {
			// this must have opposite color of its two children
			// assert (left != null) && (right != null);
			// assert (isBlack(this) && isRed(left) && isRed(right))
			//    || (isRed(this) && isBlack(left) && isBlack(right));
			return mkRed(key, value, size, left.flipColor(), right.flipColor());
		}
	}

	/**
	 * Determines if the given node is red.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is red
	 */
	private static <K,V> boolean isRed(Node<K,V> x) {
		return x != null && x instanceof RedNode<?,?>;
	}

	/**
	 * Determines if the given node is black.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is black
	 */
	private static <K,V> boolean isBlack(Node<K,V> x) {
		return x == null || x instanceof BlackNode<?,?>;
	}

	/**
	 * Yields the number of nodes in the subtree rooted at x.
	 * 
	 * @param x the root of the subtree
	 * @return the number of nodes. Yields 0 if {@code x} is {@code null}
	 */
	private static <K,V> int size(Node<K,V> x) {
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

	@Override
	public @View V get(Object key) {
		if (key == null) throw new IllegalArgumentException("key is null");
		return get(root, key);
	}

	/**
	 * Yields the value associated with the given key in subtree rooted at x;
	 * 
	 * @param x the root of the subtree
	 * @param key the key
	 * @return the value. Yields {@code null} if the key is not found
	 */
	private static <K,V> V get(Node<K,V> x, Object key) {
		while (x != null) {
			int cmp = compareTo(key, x.key);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return x.value;
		}
		return null;
	}

	@Override
	public @View V getOrDefault(Object key, V _default) {
		if (key == null) throw new IllegalArgumentException("key is null");
		return getOrDefault(root, key, _default);
	}

	private static <K,V> V getOrDefault(Node<K,V> x, Object key, V _default) {
		while (x != null) {
			int cmp = compareTo(key, x.key);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return x.value;
		}
		return _default;
	}

	@Override
	public V getOrDefault(Object key, Supplier<? extends V> _default) {
		if (key == null) throw new IllegalArgumentException("key is null");
		return getOrDefault(root, key, _default);
	}

	// value associated with the given key in subtree rooted at x; uses supplier if no such key is found
	private static <K,V> V getOrDefault(Node<K,V> x, Object key, Supplier<? extends V> _default) {
		while (x != null) {
			int cmp = compareTo(key, x.key);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return x.value;
		}
		return _default.get();
	}

	@Override
	public @View boolean containsKey(Object key) {
		return containsKey(root, key);
	}

	/**
	 * Checks if the given key is contained in the subtree rooted at x.
	 * 
	 * @param x the root of the subtree
	 * @param key the key
	 * @return true if and only if that condition holds
	 */
	private static <K,V> boolean containsKey(Node<K,V> x, Object key) {
		while (x != null) {
			int cmp = compareTo(key, x.key);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return true;
		}
		return false;
	}

	@Override
	public void put(K key, V value) {
		if (key == null) throw new IllegalArgumentException("key is null");
		root = put(root, key, value);
		mkRootBlack();
	}

	// insert the key-value pair in the subtree rooted at h
	private static <K,V> Node<K,V> put(Node<K,V> h, K key, V value) { 
		if (h == null) return Node.mkRed(key, value, 1);

		int cmp = compareTo(key, h.key);
		if      (cmp < 0) h = h.setLeft(put(h.left, key, value)); 
		else if (cmp > 0) h = h.setRight(put(h.right, key, value));
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

		// if both children of root are black, set root to red
		if (isBlack(root.left) && isBlack(root.right))
			mkRootRed();

		root = removeMin(root);
		if (!isEmpty()) mkRootBlack();
	}

	// delete the key-value pair with the minimum key rooted at h
	private static <K,V> Node<K,V> removeMin(Node<K,V> h) { 
		if (h.left == null)
			return null;

		if (isBlack(h.left) && isBlack(h.left.left))
			h = h.moveRedLeft();

		return h.setLeft(removeMin(h.left)).balance();
	}

	@Override
	public void removeMax() {
		if (isEmpty()) throw new NoSuchElementException();

		// if both children of root are black, set root to red
		if (isBlack(root.left) && isBlack(root.right))
			mkRootRed();

		root = removeMax(root);
		if (!isEmpty()) mkRootBlack();
	}

	// delete the key-value pair with the maximum key rooted at h
	private static <K,V> Node<K,V> removeMax(Node<K,V> h) { 
		if (isRed(h.left))
			h = h.rotateRight();

		if (h.right == null)
			return null;

		if (isBlack(h.right) && isBlack(h.right.left))
			h = h.moveRedRight();

		return h.setRight(removeMax(h.right)).balance();
	}

	@Override
	public void remove(Object key) { 
		if (key == null) throw new IllegalArgumentException("key is null");
		if (containsKey(key)) {
			// if both children of root are black, set root to red
			if (isBlack(root.left) && isBlack(root.right))
				mkRootRed();

			root = remove(root, key);
			if (!isEmpty()) mkRootBlack();
		}
	}

	// delete the key-value pair with the given key rooted at h
	private static <K,V> Node<K,V> remove(Node<K,V> h, Object key) { 
		// assert get(h, key) != null;

		if (compareTo(key, h.key) < 0)  {
			if (isBlack(h.left) && isBlack(h.left.left))
				h = h.moveRedLeft();

			h = h.setLeft(remove(h.left, key));
		}
		else {
			if (isRed(h.left))
				h = h.rotateRight();
			if (compareTo(key, h.key) == 0 && (h.right == null))
				return null;
			if (isBlack(h.right) && isBlack(h.right.left))
				h = h.moveRedRight();
			if (compareTo(key, h.key) == 0) {
				Node<K,V> x = min(h.right);
				if (isRed(h))
					h = Node.mkRed(x.key, x.value, h.size, h.left, removeMin(h.right));
				else
					h = Node.mkBlack(x.key, x.value, h.size, h.left, removeMin(h.right));
			}
			else
				h = h.setRight(remove(h.right, key));
		}
		return h.balance();
	}

	@Override
	public @View K min() {
		if (isEmpty()) throw new NoSuchElementException("calls min() with empty symbol table");
		return min(root).key;
	} 

	// the smallest key in subtree rooted at x
	private static <K,V> Node<K,V> min(Node<K,V> x) { 
		// assert x != null;
		if (x.left == null) return x; 
		else                return min(x.left); 
	} 

	@Override
	public @View K max() {
		if (isEmpty()) throw new NoSuchElementException("calls max() with empty symbol table");
		return max(root).key;
	} 

	// the largest key in the subtree rooted at x
	private static <K,V> Node<K,V> max(Node<K,V> x) { 
		// assert x != null;
		if (x.right == null) return x; 
		else                 return max(x.right); 
	}

	@Override
	public @View K floorKey(K key) {
		if (key == null) throw new IllegalArgumentException("key is null");
		if (isEmpty()) throw new NoSuchElementException();
		Node<K,V> x = floorKey(root, key);
		if (x == null) throw new NoSuchElementException();
		else           return x.key;
	}    

	// the largest key in the subtree rooted at x less than or equal to the given key
	private static <K,V> Node<K,V> floorKey(Node<K,V> x, K key) {
		if (x == null) return null;
		int cmp = compareTo(key, x.key);
		if (cmp == 0) return x;
		if (cmp < 0)  return floorKey(x.left, key);
		Node<K,V> t = floorKey(x.right, key);
		if (t != null) return t; 
		else           return x;
	}

	@Override
	public @View K ceilingKey(K key) {
		if (key == null) throw new IllegalArgumentException("key is null");
		if (isEmpty()) throw new NoSuchElementException();
		Node<K,V> x = ceilingKey(root, key);
		if (x == null) throw new NoSuchElementException();
		else           return x.key;  
	}

	// the smallest key in the subtree rooted at x greater than or equal to the given key
	private static <K,V> Node<K,V> ceilingKey(Node<K,V> x, K key) {  
		if (x == null) return null;
		int cmp = compareTo(key, x.key);
		if (cmp == 0) return x;
		if (cmp > 0)  return ceilingKey(x.right, key);
		Node<K,V> t = ceilingKey(x.left, key);
		if (t != null) return t; 
		else           return x;
	}

	@Override
	public @View K select(int k) {
		if (k < 0 || k >= size()) throw new IllegalArgumentException("argument to select() is invalid: " + k);
		return select(root, k).key;
	}

	// the key of rank k in the subtree rooted at x
	private static <K,V> Node<K,V> select(Node<K,V> x, int k) {
		// assert x != null;
		// assert k >= 0 && k < size(x);
		int t = size(x.left); 
		if      (t > k) return select(x.left,  k); 
		else if (t < k) return select(x.right, k-t-1); 
		else            return x; 
	} 

	@Override
	public @View int rank(K key) {
		if (key == null) throw new IllegalArgumentException("key is null");
		return rank(key, root);
	} 

	// number of keys less than key in the subtree rooted at x
	private static <K,V> int rank(K key, Node<K,V> x) {
		if (x == null) return 0; 
		int cmp = compareTo(key, x.key); 
		if      (cmp < 0) return rank(key, x.left); 
		else if (cmp > 0) return 1 + size(x.left) + rank(key, x.right); 
		else              return size(x.left); 
	} 

	@Override
	public void update(K key, UnaryOperator<V> how) {
		if (key == null) throw new IllegalArgumentException("key is null");
		root = update(root, key, how);
		mkRootBlack();
	}

	private static <K,V> Node<K,V> update(Node<K,V> h, K key, UnaryOperator<V> how) { 
		if (h == null) return Node.mkRed(key, how.apply(null), 1);

		int cmp = compareTo(key, h.key);
		if      (cmp < 0) h = h.setLeft(update(h.left,  key, how)); 
		else if (cmp > 0) h = h.setRight(update(h.right, key, how)); 
		else              h = h.setValue(how.apply(h.value));

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = h.rotateLeft();
		if (isRed(h.left)  &&  isRed(h.left.left)) h = h.rotateRight();
		if (isRed(h.left)  &&  isRed(h.right))     h = h.flipColors();

		return h.fixSize();
	}

	@Override
	public void update(K key, V _default, UnaryOperator<V> how) {
		if (key == null) throw new IllegalArgumentException("key is null");
		root = update(root, key, _default, how);
		mkRootBlack();
	}

	private static <K,V> Node<K,V> update(Node<K,V> h, K key, V _default, UnaryOperator<V> how) { 
		if (h == null) return Node.mkRed(key, how.apply(_default), 1);

		int cmp = compareTo(key, h.key);
		if      (cmp < 0) h = h.setLeft(update(h.left, key, _default, how)); 
		else if (cmp > 0) h = h.setRight(update(h.right, key, _default, how));
		else if (h.value == null)
			h = h.setValue(how.apply(_default));
		else
			h = h.setValue(how.apply(h.value));

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = h.rotateLeft();
		if (isRed(h.left)  &&  isRed(h.left.left)) h = h.rotateRight();
		if (isRed(h.left)  &&  isRed(h.right))     h = h.flipColors();

		return h.fixSize();
	}

	@Override
	public void update(K key, Supplier<? extends V> _default, UnaryOperator<V> how) {
		if (key == null) throw new IllegalArgumentException("key is null");
		root = update(root, key, _default, how);
		mkRootBlack();
	}

	private static <K,V> Node<K,V> update(Node<K,V> h, K key, Supplier<? extends V> _default, UnaryOperator<V> how) { 
		if (h == null) return Node.mkRed(key, how.apply(_default.get()), 1);

		int cmp = compareTo(key, h.key);
		if      (cmp < 0) h = h.setLeft(update(h.left, key, _default, how)); 
		else if (cmp > 0) h = h.setRight(update(h.right, key, _default, how));
		else if (h.value == null)
			h = h.setValue(how.apply(_default.get()));
		else
			h = h.setValue(how.apply(h.value));

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = h.rotateLeft();
		if (isRed(h.left)  &&  isRed(h.left.left)) h = h.rotateRight();
		if (isRed(h.left)  &&  isRed(h.right))     h = h.flipColors();

		return h.fixSize();
	}

	@Override
	public V putIfAbsent(K key, V value) {
		if (key == null) throw new IllegalArgumentException("key is null");

		class PutIfAbsent {
			private V result;

			private Node<K,V> putIfAbsent(Node<K,V> h) {
				// not found: result remains null
				if (h == null)
					// not found
					return Node.mkRed(key, value, 1);

				int cmp = compareTo(key, h.key);
				if      (cmp < 0) h = h.setLeft(putIfAbsent(h.left));
				else if (cmp > 0) h = h.setRight(putIfAbsent(h.right));
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

				return h.fixSize();
			}
		}

		PutIfAbsent pia = new PutIfAbsent();
		root = pia.putIfAbsent(root);
		mkRootBlack();

		return pia.result;
	}

	@Override
	public V computeIfAbsent(K key, Supplier<? extends V> supplier) {
		if (key == null) throw new IllegalArgumentException("key is null");

		class ComputeIfAbsent {
			private V result;

			private Node<K,V> computeIfAbsent(Node<K,V> h) { 
				if (h == null)
					// not found
					return Node.mkRed(key, result = supplier.get(), 1);

				int cmp = compareTo(key, h.key);
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

				return h.fixSize();
			}
		}

		ComputeIfAbsent cia = new ComputeIfAbsent();
		root = cia.computeIfAbsent(root);
		mkRootBlack();

		return cia.result;
	}

	@Override
	public V computeIfAbsent(K key, Function<? super K, ? extends V> supplier) {
		if (key == null) throw new IllegalArgumentException("key is null");

		class ComputeIfAbsent {
			private V result;

			private Node<K,V> computeIfAbsent(Node<K,V> h) { 
				if (h == null)
					// not found
					return Node.mkRed(key, result = supplier.apply(key), 1);

				int cmp = compareTo(key, h.key);
				if      (cmp < 0) h = h.setLeft(computeIfAbsent(h.left));
				else if (cmp > 0) h = h.setRight(computeIfAbsent(h.right));
				else if (h.value == null) {
					// found but was bound to null
					h = h.setValue(supplier.apply(key));
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

				return h.fixSize();
			}
		}

		ComputeIfAbsent cia = new ComputeIfAbsent();
		root = cia.computeIfAbsent(root);
		mkRootBlack();

		return cia.result;
	}

	@Override
	public void clear() {
		root = null;
	}

	@Override
	public Iterator<Entry<K,V>> iterator() {
		return new StorageMapIterator<K,V>(root);
	}

	private static class StorageMapIterator<K,V> implements Iterator<Entry<K,V>> {
		// the path under enumeration; it holds that the left children
		// have already been enumerated
		private final List<Node<K,V>> stack = new ArrayList<>();

		private StorageMapIterator(Node<K,V> root) {
			// initially, the stack contains the leftmost path of the tree
			for (Node<K,V> cursor = root; cursor != null; cursor = cursor.left)
				stack.add(cursor);
		}

		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		@Override
		public Entry<K,V> next() {
			Node<K,V> topmost = stack.remove(stack.size() - 1);

			// we add the leftmost path of the right child of topmost
			for (Node<K,V> cursor = topmost.right; cursor != null; cursor = cursor.left)
				stack.add(cursor);

			return topmost;
		}
	}

	@Override
	public Stream<Entry<K,V>> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	@Override
	public List<K> keyList() {
		List<K> keys = new ArrayList<>();
		if (root != null)
			keyList(root, keys);

		return keys;
	}

	private static <K,V> void keyList(Node<K,V> x, List<K> keys) {
		if (x.left != null)
			keyList(x.left, keys);

		keys.add(x.key);

		if (x.right != null)
			keyList(x.right, keys);
	}

	@Override
	public Stream<K> keys() {
		return stream().map(Entry::getKey);
	}

	@Override
	public StorageMapView<K,V> view() {

		/**
		 * A read-only view of a parent storage map. A view contains the same bindings
		 * as the parent storage map, but does not include modification methods.
		 * Moreover, a view is exported, so that it can be safely divulged outside
		 * the store of a node. Calls to the view are simply forwarded to the parent map.
		 */

		@Exported
		class StorageMapViewImpl extends Storage implements StorageMapView<K,V> {

			@Override
			public @View int size() {
				return StorageTreeMap.this.size();
			}

			@Override
			public @View boolean isEmpty() {
				return StorageTreeMap.this.isEmpty();
			}

			@Override
			public @View boolean containsKey(Object value) {
				return StorageTreeMap.this.containsKey(value);
			}

			@Override
			public Iterator<Entry<K, V>> iterator() {
				return StorageTreeMap.this.iterator();
			}

			@Override
			public V get(Object key) {
				return StorageTreeMap.this.get(key);
			}

			@Override
			public V getOrDefault(Object key, V _default) {
				return StorageTreeMap.this.getOrDefault(key, _default);
			}

			@Override
			public V getOrDefault(Object key, Supplier<? extends V> _default) {
				return StorageTreeMap.this.getOrDefault(key, _default);
			}

			@Override
			public K min() {
				return StorageTreeMap.this.min();
			}

			@Override
			public K max() {
				return StorageTreeMap.this.max();
			}

			@Override
			public K floorKey(K key) {
				return StorageTreeMap.this.floorKey(key);
			}

			@Override
			public K ceilingKey(K key) {
				return StorageTreeMap.this.ceilingKey(key);
			}

			@Override
			public K select(int k) {
				return StorageTreeMap.this.select(k);
			}

			@Override
			public int rank(K key) {
				return StorageTreeMap.this.rank(key);
			}

			@Override
			public Stream<Entry<K, V>> stream() {
				return StorageTreeMap.this.stream();
			}

			@Override
			public List<K> keyList() {
				return StorageTreeMap.this.keyList();
			}

			@Override
			public Stream<K> keys() {
				return StorageTreeMap.this.keys();
			}

			@Override
			public StorageMapView<K, V> snapshot() {
				return StorageTreeMap.this.snapshot();
			}
		}

		return new StorageMapViewImpl();
	}

	@Override
	public StorageMapView<K,V> snapshot() {
		return new StorageTreeMap<>(this).view();
	}
}