package io.takamaka.code.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
 * are kept in chronological order.
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
 * {@code compareTo()} and method to compare two keys. It does not call either
 * {@code equals()} or {@code hashCode()}.
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

public class StorageMap<K,V> extends Storage implements Iterable<StorageMap.Entry<K,V>> {
	private static final boolean RED   = true;
	private static final boolean BLACK = false;

	/**
	 * The root of the tree.
	 */
	private Node<K,V> root;

	/**
	 * A key/value pair.
	 *
	 * @param <K> the type of the keys
	 * @param <V> the type of the values
	 */
	public interface Entry<K,V> {
		K getKey();
		V getValue();
	}

	/**
	 * A node of the binary search tree that implements the map.
	 */
	private static class Node<K,V> extends Storage implements Entry<K,V> {
		private K key; // always non-null
		private V value; // possibly null
		private Node<K,V> left, right;
		private boolean color;

		/**
		 * Count of the subtree nodes.
		 */
		private int size;

		private Node(K key, V value, boolean color, int size) {
			this.key = key;
			this.value = value;
			this.color = color;
			this.size = size;
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
		public int hashCode() { // to allow to store Nodes inside Java lists
			return 42;
		}
	}

	/**
	 * Builds an empty map.
	 */
	public StorageMap() {}

	/**
	 * Determines if the given node is red.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is red
	 */
	private static <K,V> boolean isRed(Node<K,V> x) {
		return x != null && x.color == RED;
	}

	/**
	 * Determines if the given node is black.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is black
	 */
	private static <K,V> boolean isBlack(Node<K,V> x) {
		return x == null || x.color == BLACK;
	}

	/**
	 * Yields the number of nodes in the subtree rooted at x.
	 * 
	 * @param x the root of the subtree
	 * @return the number of nodes. Yields 0 if {@code x} is {@code null}
	 */
	private static <K,V> int size(Node<K,V> x) {
		if (x == null) return 0;
		return x.size;
	}

	/**
	 * Returns the number of key-value pairs in this symbol table.
	 * 
	 * @return the number of key-value pairs in this symbol table
	 */
	public @View int size() {
		return size(root);
	}

	/**
	 * Determines if this symbol table empty.
	 * 
	 * @return {@code true} if and only if this symbol table is empty
	 */
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
	 * Yields the value associated with the given key, if any.
	 * 
	 * @param key the key
	 * @return the value associated with the given key if the key is in the symbol table
	 *         and {@code null} if the key is not in the symbol table
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
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

	/**
	 * Returns the value associated with the given key.
	 * 
	 * @param key the key
	 * @return the value associated with the given key if the key is in the symbol table.
	 *         Yields {@code _default} if the key is not in the symbol table
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
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

	/**
	 * Yields the value associated with the given key.
	 * 
	 * @param key the key
	 * @return the value associated with the given key if the key is in the symbol table.
	 *         Yields {@code _default.get()} if the key is not in the symbol table
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public V getOrDefault(Object key, Supplier<V> _default) {
		if (key == null) throw new IllegalArgumentException("key is null");
		return getOrDefault(root, key, _default);
	}

	// value associated with the given key in subtree rooted at x; uses supplier if no such key is found
	private static <K,V> V getOrDefault(Node<K,V> x, Object key, Supplier<V> _default) {
		while (x != null) {
			int cmp = compareTo(key, x.key);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return x.value;
		}
		return _default.get();
	}

	/**
	 * Determines if this symbol table contain the given key.
	 * 
	 * @param key the key
	 * @return {@code true} if and only if this symbol table contains {@code key}
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public @View boolean contains(Object key) {
		return get(key) != null;
	}

	/**
	 * Inserts the specified key-value pair into this symbol table, overwriting the old 
	 * value with the new value if the symbol table already contains the specified key.
	 *
	 * @param key the key
	 * @param value the value
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public void put(K key, V value) {
		if (key == null) throw new IllegalArgumentException("key is null");
		root = put(root, key, value);
		root.color = BLACK;
		// assert check();
	}

	// insert the key-value pair in the subtree rooted at h
	private static <K,V> Node<K,V> put(Node<K,V> h, K key, V value) { 
		if (h == null) return new Node<>(key, value, RED, 1);

		int cmp = compareTo(key, h.key);
		if      (cmp < 0) h.left  = put(h.left,  key, value); 
		else if (cmp > 0) h.right = put(h.right, key, value); 
		else              h.value = value;

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
		if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);
		h.size = size(h.left) + size(h.right) + 1;

		return h;
	}

	/**
	 * Removes the smallest key and associated value from the symbol table.
	 * 
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	public void removeMin() {
		if (isEmpty()) throw new NoSuchElementException();

		// if both children of root are black, set root to red
		if (isBlack(root.left) && isBlack(root.right))
			root.color = RED;

		root = removeMin(root);
		if (!isEmpty()) root.color = BLACK;
		// assert check();
	}

	// delete the key-value pair with the minimum key rooted at h
	private static <K,V> Node<K,V> removeMin(Node<K,V> h) { 
		if (h.left == null)
			return null;

		if (isBlack(h.left) && isBlack(h.left.left))
			h = moveRedLeft(h);

		h.left = removeMin(h.left);
		return balance(h);
	}

	/**
	 * Removes the largest key and associated value from the symbol table.
	 * 
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	public void removeMax() {
		if (isEmpty()) throw new NoSuchElementException();

		// if both children of root are black, set root to red
		if (isBlack(root.left) && isBlack(root.right))
			root.color = RED;

		root = removeMax(root);
		if (!isEmpty()) root.color = BLACK;
		// assert check();
	}

	// delete the key-value pair with the maximum key rooted at h
	private static <K,V> Node<K,V> removeMax(Node<K,V> h) { 
		if (isRed(h.left))
			h = rotateRight(h);

		if (h.right == null)
			return null;

		if (isBlack(h.right) && isBlack(h.right.left))
			h = moveRedRight(h);

		h.right = removeMax(h.right);

		return balance(h);
	}

	/**
	 * Removes the specified key and its associated value from this symbol table     
	 * (if the key is in this symbol table).    
	 *
	 * @param  key the key
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public void remove(Object key) { 
		if (key == null) throw new IllegalArgumentException("key is null");
		if (contains(key)) {
			// if both children of root are black, set root to red
			if (isBlack(root.left) && isBlack(root.right))
				root.color = RED;

			root = remove(root, key);
			if (!isEmpty()) root.color = BLACK;
			// assert check();
		}
	}

	// delete the key-value pair with the given key rooted at h
	private static <K,V> Node<K,V> remove(Node<K,V> h, Object key) { 
		// assert get(h, key) != null;

		if (compareTo(key, h.key) < 0)  {
			if (isBlack(h.left) && isBlack(h.left.left))
				h = moveRedLeft(h);
			h.left = remove(h.left, key);
		}
		else {
			if (isRed(h.left))
				h = rotateRight(h);
			if (compareTo(key, h.key) == 0 && (h.right == null))
				return null;
			if (isBlack(h.right) && isBlack(h.right.left))
				h = moveRedRight(h);
			if (compareTo(key, h.key) == 0) {
				Node<K,V> x = min(h.right);
				h.key = x.key;
				h.value = x.value;
				h.right = removeMin(h.right);
			}
			else h.right = remove(h.right, key);
		}
		return balance(h);
	}

	// make a left-leaning link lean to the right
	private static <K,V> Node<K,V> rotateRight(Node<K,V> h) {
		// assert (h != null) && isRed(h.left);
		Node<K,V> x = h.left;
		h.left = x.right;
		x.right = h;
		x.color = h.color;
		h.color = RED;
		x.size = h.size;
		h.size = size(h.left) + size(h.right) + 1;
		return x;
	}

	// make a right-leaning link lean to the left
	private static <K,V> Node<K,V> rotateLeft(Node<K,V> h) {
		// assert (h != null) && isRed(h.right);
		Node<K,V> x = h.right;
		h.right = x.left;
		x.left = h;
		x.color = h.color;
		h.color = RED;
		x.size = h.size;
		h.size = size(h.left) + size(h.right) + 1;
		return x;
	}

	// flip the colors of a node and its two children
	private static <K,V> void flipColors(Node<K,V> h) {
		// h must have opposite color of its two children
		// assert (h != null) && (h.left != null) && (h.right != null);
		// assert (isBlack(h) &&  isRed(h.left) &&  isRed(h.right))
		//    || (isRed(h)  && isBlack(h.left) && isBlack(h.right));
		h.color = !h.color;
		h.left.color = !h.left.color;
		h.right.color = !h.right.color;
	}

	// Assuming that h is red and both h.left and h.left.left
	// are black, make h.left or one of its children red.
	private static <K,V> Node<K,V> moveRedLeft(Node<K,V> h) {
		// assert (h != null);
		// assert isRed(h) && isBlack(h.left) && isBlack(h.left.left);

		flipColors(h);
		if (isRed(h.right.left)) { 
			h.right = rotateRight(h.right);
			flipColors(h = rotateLeft(h));
		}
		return h;
	}

	// Assuming that h is red and both h.right and h.right.left
	// are black, make h.right or one of its children red.
	private static <K,V> Node<K,V> moveRedRight(Node<K,V> h) {
		// assert (h != null);
		// assert isRed(h) && isBlack(h.right) && isBlack(h.right.left);
		flipColors(h);
		if (isRed(h.left.left))
			flipColors(h = rotateRight(h));

		return h;
	}

	// restore red-black tree invariant
	private static <K,V> Node<K,V> balance(Node<K,V> h) {
		// assert (h != null);

		if (isRed(h.right))                      h = rotateLeft(h);
		if (isRed(h.left) && isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left) && isRed(h.right))     flipColors(h);

		h.size = size(h.left) + size(h.right) + 1;
		return h;
	}

	/**
	 * Yields the smallest key in the symbol table.
	 * 
	 * @return the smallest key in the symbol table
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	public @View K min() {
		if (isEmpty()) throw new NoSuchElementException("calls min() with empty symbol table");
		return min(root).key;
	} 

	// the smallest key in subtree rooted at x; null if no such key
	private static <K,V> Node<K,V> min(Node<K,V> x) { 
		// assert x != null;
		if (x.left == null) return x; 
		else                return min(x.left); 
	} 

	/**
	 * Yields the largest key in the symbol table.
	 * 
	 * @return the largest key in the symbol table
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	public @View K max() {
		if (isEmpty()) throw new NoSuchElementException("calls max() with empty symbol table");
		return max(root).key;
	} 

	// the largest key in the subtree rooted at x; null if no such key
	private static <K,V> Node<K,V> max(Node<K,V> x) { 
		// assert x != null;
		if (x.right == null) return x; 
		else                 return max(x.right); 
	}

	/**
	 * Yields the largest key in the symbol table less than or equal to {@code key}.
	 * 
	 * @param key the key
	 * @return the largest key in the symbol table less than or equal to {@code key}
	 * @throws NoSuchElementException if there is no such key
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
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

	/**
	 * Yields the smallest key in the symbol table greater than or equal to {@code key}.
	 * 
	 * @param key the key
	 * @return the smallest key in the symbol table greater than or equal to {@code key}
	 * @throws NoSuchElementException if there is no such key
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
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

	/**
	 * Yields the key in the symbol table whose rank is {@code k}.
	 * This is the (k+1)st smallest key in the symbol table. 
	 *
	 * @param  k the order statistic
	 * @return the key in the symbol table of rank {@code k}
	 * @throws IllegalArgumentException unless {@code k} is between 0 and {@code size()-1}
	 */
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

	/**
	 * Yields the number of keys in the symbol table strictly less than {@code key}.
	 * 
	 * @param key the key
	 * @return the number of keys in the symbol table strictly less than {@code key}
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
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

	/**
	 * Replaces the old value {@code e} at {@code key} with {@code how.apply(e)}.
	 * If {@code key} was unmapped, it will be replaced with {@code how.apply(null)},
	 * which might well lead to a run-time exception.
	 *
	 * @param key the key whose value must be replaced
	 * @param how the replacement function
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public void update(K key, UnaryOperator<V> how) {
		if (key == null) throw new IllegalArgumentException("key is null");
		root = update(root, key, how);
		root.color = BLACK;
	}

	private static <K,V> Node<K,V> update(Node<K,V> h, K key, UnaryOperator<V> how) { 
		if (h == null) return new Node<>(key, how.apply(null), RED, 1);

		int cmp = compareTo(key, h.key);
		if      (cmp < 0) h.left  = update(h.left,  key, how); 
		else if (cmp > 0) h.right = update(h.right, key, how); 
		else              h.value = how.apply(h.value);

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
		if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);
		h.size = size(h.left) + size(h.right) + 1;

		return h;
	}

	/**
	 * Replaces the old value {@code e} at {@code key} with {@code how.apply(e)}.
	 * If {@code key} was unmapped, it will be replaced with {@code how.apply(_default)}.
	 *
	 * @param key the key whose value must be replaced
	 * @param _default the default value
	 * @param how the replacement function
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public void update(K key, V _default, UnaryOperator<V> how) {
		if (key == null) throw new IllegalArgumentException("key is null");
		root = update(root, key, _default, how);
		root.color = BLACK;
	}

	private static <K,V> Node<K,V> update(Node<K,V> h, K key, V _default, UnaryOperator<V> how) { 
		if (h == null) return new Node<>(key, how.apply(_default), RED, 1);

		int cmp = compareTo(key, h.key);
		if      (cmp < 0) h.left  = update(h.left, key, _default, how); 
		else if (cmp > 0) h.right = update(h.right, key, _default, how); 
		else if (h.value == null)
			h.value = how.apply(_default);
		else
			h.value = how.apply(h.value);

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
		if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);
		h.size = size(h.left) + size(h.right) + 1;

		return h;
	}

	/**
	 * Replaces the old value {@code e} at {@code key} with {@code how.apply(e)}.
	 * If {@code key} was unmapped, it will be replaced with {@code how.apply(_default.get())}.
	 *
	 * @param key the key whose value must be replaced
	 * @param _default the supplier of the default value
	 * @param how the replacement function
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public void update(K key, Supplier<V> _default, UnaryOperator<V> how) {
		if (key == null) throw new IllegalArgumentException("key is null");
		root = update(root, key, _default, how);
		root.color = BLACK;
	}

	private static <K,V> Node<K,V> update(Node<K,V> h, K key, Supplier<V> _default, UnaryOperator<V> how) { 
		if (h == null) return new Node<>(key, how.apply(_default.get()), RED, 1);

		int cmp = compareTo(key, h.key);
		if      (cmp < 0) h.left  = update(h.left, key, _default, how); 
		else if (cmp > 0) h.right = update(h.right, key, _default, how); 
		else if (h.value == null)
			h.value = how.apply(_default.get());
		else
			h.value = how.apply(h.value);

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
		if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);
		h.size = size(h.left) + size(h.right) + 1;

		return h;
	}

	/**
	 * If the given key is unmapped or is mapped to {@code null}, map it to the given value.
	 * 
	 * @param key the key
	 * @param value the value
	 * @return the previous value at the given key. Yields {@code null} if {@code key} was previously unmapped
	 *         or was mapped to {@code null}
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public V putIfAbsent(K key, V value) {
		if (key == null) throw new IllegalArgumentException("key is null");

		class PutIfAbsent {
			private V result;

			private Node<K,V> putIfAbsent(Node<K,V> h) {
				// not found: result remains null
				if (h == null)
					// not found
					return new Node<>(key, value, RED, 1);

				int cmp = compareTo(key, h.key);
				if      (cmp < 0) h.left  = putIfAbsent(h.left);
				else if (cmp > 0) h.right = putIfAbsent(h.right);
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
				h.size = size(h.left) + size(h.right) + 1;

				return h;
			}
		}

		PutIfAbsent pia = new PutIfAbsent();
		root = pia.putIfAbsent(root);
		root.color = BLACK;

		return pia.result;
	}

	/**
	 * If the given key is unmapped or is mapped to {@code null}, map it to the value given by a supplier.
	 * 
	 * @param key the key
	 * @param supplier the supplier
	 * @return the previous value at the given key, if it was already mapped to a non-{@code null} value.
	 *         If the key was unmapped or was mapped to {@code null}, yields the new value
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public V computeIfAbsent(K key, Supplier<V> supplier) {
		if (key == null) throw new IllegalArgumentException("key is null");

		class ComputeIfAbsent {
			private V result;

			private Node<K,V> computeIfAbsent(Node<K,V> h) { 
				if (h == null)
					// not found
					return new Node<>(key, result = supplier.get(), RED, 1);

				int cmp = compareTo(key, h.key);
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
				h.size = size(h.left) + size(h.right) + 1;

				return h;
			}
		}

		ComputeIfAbsent cia = new ComputeIfAbsent();
		root = cia.computeIfAbsent(root);
		root.color = BLACK;

		return cia.result;
	}

	/**
	 * If the given key is unmapped or is mapped to {@code null}, map it to the value given by a supplier.
	 * 
	 * @param key the key
	 * @param supplier the supplier
	 * @return the previous value at the given key, if it was already mapped to a non-{@code null} value.
	 *         If the key was unmapped or was mapped to {@code null}, yields the new value
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public V computeIfAbsent(K key, Function<K,V> supplier) {
		if (key == null) throw new IllegalArgumentException("key is null");

		class ComputeIfAbsent {
			private V result;

			private Node<K,V> computeIfAbsent(Node<K,V> h) { 
				if (h == null)
					// not found
					return new Node<>(key, result = supplier.apply(key), RED, 1);

				int cmp = compareTo(key, h.key);
				if      (cmp < 0) h.left  = computeIfAbsent(h.left);
				else if (cmp > 0) h.right = computeIfAbsent(h.right);
				else if (h.value == null) {
					// found but was bound to null
					result = h.value = supplier.apply(key);
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
				h.size = size(h.left) + size(h.right) + 1;

				return h;
			}
		}

		ComputeIfAbsent cia = new ComputeIfAbsent();
		root = cia.computeIfAbsent(root);
		root.color = BLACK;

		return cia.result;
	}

	@Override
	public Iterator<Entry<K,V>> iterator() {
		return new StorageMapIterator<K,V>(root);
	}

	private static class StorageMapIterator<K,V> implements Iterator<Entry<K,V>> {
		// the path under enumeration; it holds that the left children
		// have already been enumerated
		private List<Node<K,V>> stack = new ArrayList<>();

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

	/**
	 * Yields an ordered stream of the entries (key/value) in this map, in
	 * increasing order of keys.
	 * 
	 * @return the stream
	 */
	public Stream<Entry<K,V>> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * Yields the keys of this map, in increasing order.
	 * 
	 * @return the keys
	 */
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

	/**
	 * Yields the ordered stream of the keys of this map, in increasing order.
	 * 
	 * @return the stream
	 */
	public Stream<K> keys() {
		return stream().map(Entry::getKey);
	}
}