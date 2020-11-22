package io.takamaka.code.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

/**
 * A map from integer keys to (possibly {@code null}) storage values,
 * that can be kept in storage. By iterating on this object, one gets
 * the key/value pairs of the map, in increasing key order.
 *
 * This code is derived from Sedgewick and Wayne's code for
 * red-black trees, with some adaptation. It implements an associative
 * map from keys to values. The map can be kept in storage.
 * Values must have types allowed in storage. Keys are kept in increasing order.
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
 * This implementation uses a left-leaning red-black BST.
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
 * @param <V> the type of the values
 */

public class StorageTreeIntMap<V> extends Storage implements StorageIntMap<V> {

	/**
	 * Builds an empty map.
	 */
	public StorageTreeIntMap() {}

	/**
	 * Creates a map initialized to the same binings as the given parent map.
	 * 
	 * @param parent the parent map
	 */
	public StorageTreeIntMap(Map<Integer, ? extends V> parent) {
		parent.forEach(this::put);
	}

	private static final boolean RED   = true;
	private static final boolean BLACK = false;

	/**
	 * The root of the tree.
	 */
	private Node<V> root;

	/**
	 * A node of the binary search tree that implements the map.
	 */
	private static class Node<V> extends Storage implements Entry<V> {
		private int key;
		private V value; // possibly null
		private Node<V> left, right;
		private boolean color;

		/**
		 * Count of the subtree nodes.
		 */
		private int size;

		private Node(int key, V value, boolean color, int size) {
			this.key = key;
			this.value = value;
			this.color = color;
			this.size = size;
		}

		@Override
		public int getKey() {
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

	/**
	 * Yields the number of nodes in the subtree rooted at x.
	 * 
	 * @param x the root of the subtree
	 * @return the number of nodes. Yields 0 if {@code x} is {@code null}
	 */
	private static <V> int size(Node<V> x) {
		if (x == null) return 0;
		return x.size;
	}

	@Override
	public @View int size() {
		return size(root);
	}

	@Override
	public @View boolean isEmpty() {
		return root == null;
	}

	private static <K> int compareTo(int key1, int key2) {
		return key1 - key2;
	}

	@Override
	public @View V get(int key) {
		return get(root, key);
	}

	/**
	 * Yields the value associated with the given key in subtree rooted at x;
	 * 
	 * @param x the root of the subtree
	 * @param key the key
	 * @return the value. Yields {@code null} if the key is not found
	 */
	private static <V> V get(Node<V> x, int key) {
		while (x != null) {
			int cmp = compareTo(key, x.key);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return x.value;
		}
		return null;
	}

	@Override
	public @View V getOrDefault(int key, V _default) {
		return getOrDefault(root, key, _default);
	}

	private static <V> V getOrDefault(Node<V> x, int key, V _default) {
		while (x != null) {
			int cmp = compareTo(key, x.key);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return x.value;
		}
		return _default;
	}

	@Override
	public V getOrDefault(int key, Supplier<? extends V> _default) {
		return getOrDefault(root, key, _default);
	}

	// value associated with the given key in subtree rooted at x; uses supplier if no such key is found
	private static <V> V getOrDefault(Node<V> x, int key, Supplier<? extends V> _default) {
		while (x != null) {
			int cmp = compareTo(key, x.key);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return x.value;
		}
		return _default.get();
	}

	@Override
	public @View boolean contains(int key) {
		return get(key) != null;
	}

	@Override
	public void put(int key, V value) {
		root = put(root, key, value);
		root.color = BLACK;
		// assert check();
	}

	// insert the key-value pair in the subtree rooted at h
	private static <V> Node<V> put(Node<V> h, int key, V value) { 
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

	@Override
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
		// assert check();
	}

	// delete the key-value pair with the maximum key rooted at h
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
	public void remove(int key) { 
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
	private static <V> Node<V> remove(Node<V> h, int key) { 
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
				Node<V> x = min(h.right);
				h.key = x.key;
				h.value = x.value;
				h.right = removeMin(h.right);
			}
			else h.right = remove(h.right, key);
		}
		return balance(h);
	}

	// make a left-leaning link lean to the right
	private static <V> Node<V> rotateRight(Node<V> h) {
		// assert (h != null) && isRed(h.left);
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
		// assert (h != null) && isRed(h.right);
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
		// assert (h != null) && (h.left != null) && (h.right != null);
		// assert (isBlack(h) &&  isRed(h.left) &&  isRed(h.right))
		//    || (isRed(h)  && isBlack(h.left) && isBlack(h.right));
		h.color = !h.color;
		h.left.color = !h.left.color;
		h.right.color = !h.right.color;
	}

	// Assuming that h is red and both h.left and h.left.left
	// are black, make h.left or one of its children red.
	private static <V> Node<V> moveRedLeft(Node<V> h) {
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
	private static <V> Node<V> moveRedRight(Node<V> h) {
		// assert (h != null);
		// assert isRed(h) && isBlack(h.right) && isBlack(h.right.left);
		flipColors(h);
		if (isRed(h.left.left))
			flipColors(h = rotateRight(h));

		return h;
	}

	// restore red-black tree invariant
	private static <V> Node<V> balance(Node<V> h) {
		// assert (h != null);

		if (isRed(h.right))                      h = rotateLeft(h);
		if (isRed(h.left) && isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left) && isRed(h.right))     flipColors(h);

		h.size = size(h.left) + size(h.right) + 1;
		return h;
	}

	@Override
	public @View int min() {
		if (isEmpty()) throw new NoSuchElementException("calls min() with empty symbol table");
		return min(root).key;
	} 

	// the smallest key in subtree rooted at x
	private static <V> Node<V> min(Node<V> x) { 
		// assert x != null;
		if (x.left == null) return x; 
		else                return min(x.left); 
	} 

	@Override
	public @View int max() {
		if (isEmpty()) throw new NoSuchElementException("calls max() with empty symbol table");
		return max(root).key;
	} 

	// the largest key in the subtree rooted at x
	private static <V> Node<V> max(Node<V> x) { 
		// assert x != null;
		if (x.right == null) return x; 
		else                 return max(x.right); 
	}

	@Override
	public @View int floorKey(int key) {
		if (isEmpty()) throw new NoSuchElementException();
		Node<V> x = floorKey(root, key);
		if (x == null) throw new NoSuchElementException();
		else           return x.key;
	}    

	// the largest key in the subtree rooted at x less than or equal to the given key
	private static <V> Node<V> floorKey(Node<V> x, int key) {
		if (x == null) return null;
		int cmp = compareTo(key, x.key);
		if (cmp == 0) return x;
		if (cmp < 0)  return floorKey(x.left, key);
		Node<V> t = floorKey(x.right, key);
		if (t != null) return t; 
		else           return x;
	}

	@Override
	public @View int ceilingKey(int key) {
		if (isEmpty()) throw new NoSuchElementException();
		Node<V> x = ceilingKey(root, key);
		if (x == null) throw new NoSuchElementException();
		else           return x.key;  
	}

	// the smallest key in the subtree rooted at x greater than or equal to the given key
	private static <V> Node<V> ceilingKey(Node<V> x, int key) {  
		if (x == null) return null;
		int cmp = compareTo(key, x.key);
		if (cmp == 0) return x;
		if (cmp > 0)  return ceilingKey(x.right, key);
		Node<V> t = ceilingKey(x.left, key);
		if (t != null) return t; 
		else           return x;
	}

	@Override
	public @View int select(int k) {
		if (k < 0 || k >= size()) throw new IllegalArgumentException("argument to select() is invalid: " + k);
		return select(root, k).key;
	}

	// the key of rank k in the subtree rooted at x
	private static <V> Node<V> select(Node<V> x, int k) {
		// assert x != null;
		// assert k >= 0 && k < size(x);
		int t = size(x.left); 
		if      (t > k) return select(x.left,  k); 
		else if (t < k) return select(x.right, k-t-1); 
		else            return x; 
	} 

	@Override
	public @View int rank(int key) {
		return rank(key, root);
	} 

	// number of keys less than key in the subtree rooted at x
	private static <V> int rank(int key, Node<V> x) {
		if (x == null) return 0; 
		int cmp = compareTo(key, x.key); 
		if      (cmp < 0) return rank(key, x.left); 
		else if (cmp > 0) return 1 + size(x.left) + rank(key, x.right); 
		else              return size(x.left); 
	} 

	@Override
	public void update(int key, UnaryOperator<V> how) {
		root = update(root, key, how);
		root.color = BLACK;
	}

	private static <V> Node<V> update(Node<V> h, int key, UnaryOperator<V> how) { 
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

	@Override
	public void update(int key, V _default, UnaryOperator<V> how) {
		root = update(root, key, _default, how);
		root.color = BLACK;
	}

	private static <V> Node<V> update(Node<V> h, int key, V _default, UnaryOperator<V> how) { 
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

	@Override
	public void update(int key, Supplier<? extends V> _default, UnaryOperator<V> how) {
		root = update(root, key, _default, how);
		root.color = BLACK;
	}

	private static <V> Node<V> update(Node<V> h, int key, Supplier<? extends V> _default, UnaryOperator<V> how) { 
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

	@Override
	public V putIfAbsent(int key, V value) {
		class PutIfAbsent {
			private V result;

			private Node<V> putIfAbsent(Node<V> h) {
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

	@Override
	public V computeIfAbsent(int key, Supplier<? extends V> supplier) {
		class ComputeIfAbsent {
			private V result;

			private Node<V> computeIfAbsent(Node<V> h) { 
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

	@Override
	public V computeIfAbsent(int key, IntFunction<? extends V> supplier) {
		class ComputeIfAbsent {
			private V result;

			private Node<V> computeIfAbsent(Node<V> h) { 
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
	public Iterator<Entry<V>> iterator() {
		return new StorageMapIterator<V>(root);
	}

	private static class StorageMapIterator<V> implements Iterator<Entry<V>> {
		// the path under enumeration; it holds that the left children
		// have already been enumerated
		private final List<Node<V>> stack = new ArrayList<>();

		private StorageMapIterator(Node<V> root) {
			// initially, the stack contains the leftmost path of the tree
			for (Node<V> cursor = root; cursor != null; cursor = cursor.left)
				stack.add(cursor);
		}

		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		@Override
		public Entry<V> next() {
			Node<V> topmost = stack.remove(stack.size() - 1);

			// we add the leftmost path of the right child of topmost
			for (Node<V> cursor = topmost.right; cursor != null; cursor = cursor.left)
				stack.add(cursor);

			return topmost;
		}
	}

	@Override
	public Stream<Entry<V>> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	@Override
	public List<Integer> keyList() {
		List<Integer> keys = new ArrayList<>();
		if (root != null)
			keyList(root, keys);
		return keys;
	}

	private static <V> void keyList(Node<V> x, List<Integer> keys) {
		if (x.left != null)
			keyList(x.left, keys);

		keys.add(x.key);
		if (x.right != null)
			keyList(x.right, keys);
	}

	@Override
	public IntStream keys() {
		return stream().mapToInt(entry -> entry.getKey());
	}

	@Override
	public StorageIntMapView<V> view() {

		/**
		 * A read-only view of a parent storage map. A view contains the same bindings
		 * as the parent storage map, but does not include modification methods.
		 * Moreover, a view is exported, so that it can be safely divulged outside
		 * the store of a node. Calls to the view are simply forwarded to the parent map.
		 */

		@Exported
		class StorageIntMapViewImpl extends Storage implements StorageIntMapView<V> {

			@Override
			public @View int size() {
				return StorageTreeIntMap.this.size();
			}

			@Override
			public @View boolean isEmpty() {
				return StorageTreeIntMap.this.isEmpty();
			}

			@Override
			public Iterator<Entry<V>> iterator() {
				return StorageTreeIntMap.this.iterator();
			}

			@Override
			public V get(int key) {
				return StorageTreeIntMap.this.get(key);
			}

			@Override
			public V getOrDefault(int key, V _default) {
				return StorageTreeIntMap.this.getOrDefault(key, _default);
			}

			@Override
			public V getOrDefault(int key, Supplier<? extends V> _default) {
				return StorageTreeIntMap.this.getOrDefault(key, _default);
			}

			@Override
			public boolean contains(int key) {
				return StorageTreeIntMap.this.contains(key);
			}

			@Override
			public int min() {
				return StorageTreeIntMap.this.min();
			}

			@Override
			public int max() {
				return StorageTreeIntMap.this.max();
			}

			@Override
			public int floorKey(int key) {
				return StorageTreeIntMap.this.floorKey(key);
			}

			@Override
			public int ceilingKey(int key) {
				return StorageTreeIntMap.this.ceilingKey(key);
			}

			@Override
			public int select(int k) {
				return StorageTreeIntMap.this.select(k);
			}

			@Override
			public int rank(int key) {
				return StorageTreeIntMap.this.rank(key);
			}

			@Override
			public String toString() {
				return StorageTreeIntMap.this.toString();
			}

			@Override
			public Stream<Entry<V>> stream() {
				return StorageTreeIntMap.this.stream();
			}

			@Override
			public List<Integer> keyList() {
				return StorageTreeIntMap.this.keyList();
			}

			@Override
			public IntStream keys() {
				return StorageTreeIntMap.this.keys();
			}
		}

		return new StorageIntMapViewImpl();
	}

	@Override
	public StorageIntMapView<V> snapshot() {
		StorageTreeIntMap<V> copy = new StorageTreeIntMap<>();
		stream().forEachOrdered(entry -> copy.put(entry.getKey(), entry.getValue()));
		return copy.view();
	}
}