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
	 * The root of the tree.
	 */
	private Node<V> root;

	/**
	 * Builds an empty map.
	 */
	public StorageTreeIntMap() {}

	/**
	 * Creates a map initialized to the same bindings as the given parent map.
	 * 
	 * @param parent the parent map
	 */
	public StorageTreeIntMap(Map<Integer, ? extends V> parent) {
		parent.forEach(this::put);
	}

	/**
	 * Yields a snapshot of the given map.
	 * 
	 * @param parent the map
	 */
	private StorageTreeIntMap(StorageTreeIntMap<V> parent) {
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
	private abstract static class Node<V> extends Storage implements Entry<V> {
		protected final int key;
		protected final V value; // possibly null
		protected final Node<V> left, right;

		/**
		 * Count of the subtree nodes.
		 */
		protected final int size;

		private Node(int key, V value, int size, Node<V> left, Node<V> right) {
			this.key = key;
			this.value = value;
			this.size = size;
			this.left = left;
			this.right = right;
		}

		protected static <V> Node<V> mkBlack(int key, V value, int size, Node<V> left, Node<V> right) {
			return new BlackNode<>(key, value, size, left, right);
		}

		protected static <V> Node<V> mkRed(int key, V value, int size, Node<V> left, Node<V> right) {
			return new RedNode<>(key, value, size, left, right);
		}

		protected static <V> Node<V> mkRed(int key, V value) {
			return new RedNode<>(key, value, 1, null, null);
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
			// assert isRed(this) && isBlack(left) && isBlack(left.left);
			Node<V> h = flipColors();
			return isRed(h.right.left) ? h.setRight(h.right.rotateRight()).rotateLeft().flipColors() : h;
		}

		private Node<V> moveRedRight() {
			// assert isRed(this) && isBlack(right) && isBlack(right.left);
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

		private RedNode(int key, V value, int size, Node<V> left, Node<V> right) {
			super(key, value, size, left, right);
		}

		@Override
		protected Node<V> fixSize() {
			return mkRed(key, value, size(left) + size(right) + 1, left, right);
		}

		@Override
		protected Node<V> flipColor() {
			return mkBlack(key, value, size, left, right);
		}

		@Override
		protected Node<V> rotateLeft() {
			final Node<V> x = right;
			Node<V> newThis = mkRed(key, value, size(x.left) + size(left) + 1, left, x.left);
			return mkRed(x.key, x.value, size, newThis, x.right);
		}

		@Override
		protected Node<V> rotateRight() {
			// assert isRed(left);
			final Node<V> x = left;
			Node<V> newThis = mkRed(key, value, size(x.right) + size(right) + 1, x.right, right);
			return mkRed(x.key, x.value, size, x.left, newThis);
		}

		@Override
		protected Node<V> setValue(V value) {
			return mkRed(key, value, size, left, right);
		}

		@Override
		protected Node<V> setLeft(Node<V> left) {
			return mkRed(key, value, size, left, right);
		}

		@Override
		protected Node<V> setRight(Node<V> right) {
			return mkRed(key, value, size, left, right);
		}

		@Override
		protected Node<V> flipColors() {
			// h must have opposite color of its two children
			// assert (h != null) && (h.left != null) && (h.right != null);
			// assert (isBlack(h) &&  isRed(h.left) &&  isRed(h.right))
			//    || (isRed(h)  && isBlack(h.left) && isBlack(h.right));
			return mkBlack(key, value, size, left.flipColor(), right.flipColor());
		}
	}

	private static class BlackNode<V> extends Node<V> {

		private BlackNode(int key, V value, int size, Node<V> left, Node<V> right) {
			super(key, value, size, left, right);
		}

		@Override
		protected Node<V> fixSize() {
			return mkBlack(key, value, size(left) + size(right) + 1, left, right);
		}

		@Override
		protected Node<V> flipColor() {
			return mkRed(key, value, size, left, right);
		}

		@Override
		protected Node<V> rotateLeft() {
			final Node<V> x = right;
			Node<V> newThis = mkRed(key, value, size(x.left) + size(left) + 1, left, x.left);
			return mkBlack(x.key, x.value, size, newThis, x.right);
		}

		@Override
		protected Node<V> rotateRight() {
			final Node<V> x = left;
			Node<V> newThis = mkRed(key, value, size(x.right) + size(right) + 1, x.right, right);
			return mkBlack(x.key, x.value, size, x.left, newThis);
		}

		@Override
		protected Node<V> setValue(V value) {
			return mkBlack(key, value, size, left, right);
		}

		@Override
		protected Node<V> setLeft(Node<V> left) {
			return mkBlack(key, value, size, left, right);
		}

		@Override
		protected Node<V> setRight(Node<V> right) {
			return mkBlack(key, value, size, left, right);
		}

		@Override
		protected Node<V> flipColors() {
			// this must have opposite color of its two children
			// assert (left != null) && (right != null);
			// assert ( isBlack(this) && isRed(left) && isRed(right))
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

	/**
	 * Yields the number of nodes in the subtree rooted at x.
	 * 
	 * @param x the root of the subtree
	 * @return the number of nodes. Yields 0 if {@code x} is {@code null}
	 */
	private static <V> int size(Node<V> x) {
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

	private static int compareTo(int key1, int key2) {
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
	public @View boolean containsKey(int key) {
		return containsKey(root, key);
	}

	/**
	 * Checks if the given key is contained in the subtree rooted at x.
	 * 
	 * @param x the root of the subtree
	 * @param key the key
	 * @return true if and only if that condition holds
	 */
	private static <V> boolean containsKey(Node<V> x, int key) {
		while (x != null) {
			int cmp = compareTo(key, x.key);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return true;
		}
		return false;
	}

	@Override
	public void put(int key, V value) {
		root = put(root, key, value);
		mkRootBlack();
	}

	// insert the key-value pair in the subtree rooted at h
	private static <V> Node<V> put(Node<V> h, int key, V value) { 
		if (h == null) return Node.mkRed(key, value);

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

		// if both children of root are black, set root to red
		if (isBlack(root.left) && isBlack(root.right))
			mkRootRed();

		root = removeMax(root);
		if (!isEmpty()) mkRootBlack();
	}

	// delete the key-value pair with the maximum key rooted at h
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
	public void remove(int key) { 
		if (containsKey(key)) {
			// if both children of root are black, set root to red
			if (isBlack(root.left) && isBlack(root.right))
				mkRootRed();

			root = remove(root, key);
			if (!isEmpty()) mkRootBlack();
		}
	}

	// delete the key-value pair with the given key rooted at h
	private static <V> Node<V> remove(Node<V> h, int key) { 
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
				var x = min(h.right);
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
	public @View int min() {
		if (isEmpty()) throw new NoSuchElementException("call to min() with empty symbol table");
		return min(root).key;
	} 

	// the smallest key in subtree rooted at x
	private static <V> Node<V> min(Node<V> x) { 
		if (x.left == null) return x; 
		else                return min(x.left); 
	} 

	@Override
	public @View int max() {
		if (isEmpty()) throw new NoSuchElementException("call to max() with empty symbol table");
		return max(root).key;
	} 

	// the largest key in the subtree rooted at x
	private static <V> Node<V> max(Node<V> x) { 
		if (x.right == null) return x; 
		else                 return max(x.right);
	}

	@Override
	public @View int floorKey(int key) {
		if (isEmpty()) throw new NoSuchElementException();
		var x = floorKey(root, key);
		if (x == null) throw new NoSuchElementException();
		else           return x.key;
	}    

	// the largest key in the subtree rooted at x less than or equal to the given key
	private static <V> Node<V> floorKey(Node<V> x, int key) {
		if (x == null) return null;
		int cmp = compareTo(key, x.key);
		if (cmp == 0) return x;
		if (cmp < 0)  return floorKey(x.left, key);
		var t = floorKey(x.right, key);
		if (t != null) return t; 
		else           return x;
	}

	@Override
	public @View int ceilingKey(int key) {
		if (isEmpty()) throw new NoSuchElementException();
		var x = ceilingKey(root, key);
		if (x == null) throw new NoSuchElementException();
		else           return x.key; 
	}

	// the smallest key in the subtree rooted at x greater than or equal to the given key
	private static <V> Node<V> ceilingKey(Node<V> x, int key) {  
		if (x == null) return null;
		int cmp = compareTo(key, x.key);
		if (cmp == 0) return x;
		if (cmp > 0)  return ceilingKey(x.right, key);
		var t = ceilingKey(x.left, key);
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
		mkRootBlack();
	}

	private static <V> Node<V> update(Node<V> h, int key, UnaryOperator<V> how) { 
		if (h == null) return Node.mkRed(key, how.apply(null));

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
	public void update(int key, V _default, UnaryOperator<V> how) {
		root = update(root, key, _default, how);
		mkRootBlack();
	}

	private static <V> Node<V> update(Node<V> h, int key, V _default, UnaryOperator<V> how) { 
		if (h == null) return Node.mkRed(key, how.apply(_default));

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
	public void update(int key, Supplier<? extends V> _default, UnaryOperator<V> how) {
		root = update(root, key, _default, how);
		mkRootBlack();
	}

	private static <V> Node<V> update(Node<V> h, int key, Supplier<? extends V> _default, UnaryOperator<V> how) { 
		if (h == null) return Node.mkRed(key, how.apply(_default.get()));

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
	public V putIfAbsent(int key, V value) {
		class PutIfAbsent {
			private V result;

			private Node<V> putIfAbsent(Node<V> h) {
				// not found: result remains null
				if (h == null)
					// not found
					return Node.mkRed(key, value);

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
	public V computeIfAbsent(int key, Supplier<? extends V> supplier) {
		class ComputeIfAbsent {
			private V result;

			private Node<V> computeIfAbsent(Node<V> h) { 
				if (h == null)
					// not found
					return Node.mkRed(key, result = supplier.get());

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
	public V computeIfAbsent(int key, IntFunction<? extends V> supplier) {
		class ComputeIfAbsent {
			private V result;

			private Node<V> computeIfAbsent(Node<V> h) { 
				if (h == null)
					// not found
					return Node.mkRed(key, result = supplier.apply(key));

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
	public Iterator<Entry<V>> iterator() {
		return new StorageMapIterator<>(root);
	}

	private static class StorageMapIterator<V> implements Iterator<Entry<V>> {
		// the path under enumeration; it holds that the left children
		// have already been enumerated
		private final List<Node<V>> stack = new ArrayList<>();

		private StorageMapIterator(Node<V> root) {
			// initially, the stack contains the leftmost path of the tree
			for (var cursor = root; cursor != null; cursor = cursor.left)
				stack.add(cursor);
		}

		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		@Override
		public Entry<V> next() {
			var topmost = stack.remove(stack.size() - 1);

			// we add the leftmost path of the right child of topmost
			for (var cursor = topmost.right; cursor != null; cursor = cursor.left)
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
		return stream().mapToInt(Entry::getKey);
	}

	@Override
	public Stream<V> values() {
		return stream().map(Entry::getValue);
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
			public boolean containsKey(int key) {
				return StorageTreeIntMap.this.containsKey(key);
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

			@Override
			public StorageIntMapView<V> snapshot() {
				return StorageTreeIntMap.this.snapshot();
			}

			@Override
			public Stream<V> values() {
				return StorageTreeIntMap.this.values();
			}
		}

		return new StorageIntMapViewImpl();
	}

	@Override
	public StorageIntMapView<V> snapshot() {
		return new StorageTreeIntMap<>(this).view();
	}
}