package io.takamaka.code.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

/**
 * A mutable array of bytes, that can be kept in storage. Unset elements default to 0.
 * The length of the array is decided at creation time and cannot be changed later.
 * Its elements can be updated.
 * By iterating on this object, one gets its values, in increasing index order.
 *
 * This code is derived from Sedgewick and Wayne's code for
 * red-black trees, with some adaptation. It implements an associative
 * map from indexes to bytes. The map can be kept in storage.
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
 */

public class Bytes extends AbstractByteArray implements MutableByteArray {
	private static final boolean RED   = true;
	private static final boolean BLACK = false;

	/**
	 * The root of the tree.
	 */
	private Node root;

	/**
	 * The immutable size of the array.
	 */
	public final int length;

	/**
	 * A node of the binary search tree that implements the map.
	 */
	private static class Node extends Storage {
		private int index;
		private byte value;
		private Node left, right;
		private boolean color;

		private Node(int index, byte value, boolean color, int size) {
			this.index = index;
			this.value = value;
			this.color = color;
		}
	}

	/**
	 * Builds an empty array of the given length.
	 * 
	 * @param length the length of the array
	 * @throws NegativeArraySizeException if {@code length} is negative
	 */
	public Bytes(int length) {
		if (length < 0)
			throw new NegativeArraySizeException();

		this.length = length;
	}

	@Override @View
	public int length() {
		return length;
	}

	/**
	 * Determines if the given node is red.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is red
	 */
	private static boolean isRed(Node x) {
		return x != null && x.color == RED;
	}

	/**
	 * Determines if the given node is black.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is black
	 */
	private static boolean isBlack(Node x) {
		return x == null || x.color == BLACK;
	}

	private static int compareTo(int index1, int index2) {
		return index1 - index2;
	}

	@Override
	public @View byte get(int index) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index);

		return get(root, index);
	}

	/**
	 * Yields the value associated with the given index in subtree rooted at x;
	 * 
	 * @param x the root of the subtree
	 * @param index the index
	 * @return the value. Yields {@code 0} if the index is not found
	 */
	private static byte get(Node x, int index) {
		while (x != null) {
			int cmp = compareTo(index, x.index);
			if      (cmp < 0) x = x.left;
			else if (cmp > 0) x = x.right;
			else              return x.value;
		}
		return 0;
	}

	@Override
	public void set(int index, byte value) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index);

		root = set(root, index, value);
		root.color = BLACK;
	}

	// insert the index-value pair in the subtree rooted at h
	private static Node set(Node h, int index, byte value) { 
		if (h == null) return new Node(index, value, RED, 1);

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
	private static Node rotateRight(Node h) {
		// assert (h != null) && isRed(h.left);
		Node x = h.left;
		h.left = x.right;
		x.right = h;
		x.color = h.color;
		h.color = RED;
		return x;
	}

	// make a right-leaning link lean to the left
	private static Node rotateLeft(Node h) {
		// assert (h != null) && isRed(h.right);
		Node x = h.right;
		h.right = x.left;
		x.left = h;
		x.color = h.color;
		h.color = RED;
		return x;
	}

	// flip the colors of a node and its two children
	private static void flipColors(Node h) {
		// h must have opposite color of its two children
		// assert (h != null) && (h.left != null) && (h.right != null);
		// assert (isBlack(h) &&  isRed(h.left) &&  isRed(h.right))
		//    || (isRed(h)  && isBlack(h.left) && isBlack(h.right));
		h.color = !h.color;
		h.left.color = !h.left.color;
		h.right.color = !h.right.color;
	}

	@Override
	public void update(int index, IntUnaryOperator how) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index);

		root = update(root, index, how);
		root.color = BLACK;
	}

	private static Node update(Node h, int index, IntUnaryOperator how) { 
		if (h == null) return new Node(index, (byte) how.applyAsInt(0), RED, 1);

		int cmp = compareTo(index, h.index);
		if      (cmp < 0) h.left  = update(h.left,  index, how); 
		else if (cmp > 0) h.right = update(h.right, index, how); 
		else              h.value = (byte) how.applyAsInt(h.value);

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = rotateLeft(h);
		if (isRed(h.left)  &&  isRed(h.left.left)) h = rotateRight(h);
		if (isRed(h.left)  &&  isRed(h.right))     flipColors(h);

		return h;
	}

	@Override
	public Iterator<Byte> iterator() {
		return new BytesIterator(root, length);
	}

	private static class BytesIterator implements Iterator<Byte> {
		// the path under enumeration; it holds that the left children
		// have already been enumerated
		private List<Node> stack = new ArrayList<>();
		private int nextKey;
		private final int length;

		private BytesIterator(Node root, int length) {
			this.length = length;

			// initially, the stack contains the leftmost path of the tree
			for (Node cursor = root; cursor != null; cursor = cursor.left)
				stack.add(cursor);
		}

		@Override
		public boolean hasNext() {
			return nextKey < length;
		}

		@Override
		public Byte next() {
			// first check if we are in a hole of default values
			if (stack.isEmpty() || nextKey < stack.get(stack.size() - 1).index) {
				nextKey++;
				return 0;
			}

			Node topmost = stack.remove(stack.size() - 1);

			// we add the leftmost path of the right child of topmost
			for (Node cursor = topmost.right; cursor != null; cursor = cursor.left)
				stack.add(cursor);

			nextKey++;
			return topmost.value;
		}
	}

	@Override
	public IntStream stream() {
		return StreamSupport.stream(spliterator(), false).mapToInt(Byte::byteValue);
	}

	@Override
	public byte[] toArray() {
		byte[] result = new byte[length];
		int pos = 0;
		for (Byte b: this)
			result[pos++] = b;

		return result;
	}
}