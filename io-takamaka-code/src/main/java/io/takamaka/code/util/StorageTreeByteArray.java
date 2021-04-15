package io.takamaka.code.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

/**
 * A mutable array of bytes, that can be kept in storage. Elements not set default to 0.
 * The length of the array is decided at creation time and cannot be changed later.
 * Its elements can be updated.
 * By iterating on this object, one gets its values, in increasing index order.
 *
 * This code is derived from Sedgewick and Wayne's code for
 * red-black trees, with some adaptation. It implements an associative
 * map from indexes to bytes. The map can be kept in storage.
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
 */

public class StorageTreeByteArray extends AbstractStorageByteArrayView implements StorageByteArray {

	/**
	 * The root of the tree.
	 */
	private Node root;

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
	public StorageTreeByteArray(int length) {
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
	public StorageTreeByteArray(int length, byte initialValue) {
		this(length);
	
		IntStream.range(0, length).forEachOrdered(index -> set(index, initialValue));
	}

	/**
	 * Builds an array of the given length, whose elements
	 * are all initialized to the value provided by the given supplier.
	 * 
	 * @param length the length of the array
	 * @param supplier the supplier of the initial values of the array. It gets
	 *                 used repeatedly for each element to initialize. Its result
	 *                 is cast to {@code byte}
	 * @throws NegativeArraySizeException if {@code length} is negative
	 */
	public StorageTreeByteArray(int length, IntSupplier supplier) {
		this(length);
	
		IntStream.range(0, length).forEachOrdered(index -> set(index, (byte) supplier.getAsInt()));
	}

	/**
	 * Builds an array of the given length, whose elements
	 * are all initialized to the value provided by the given supplier.
	 * 
	 * @param length the length of the array
	 * @param supplier the supplier of the initial values of the array. It gets
	 *                 used repeatedly for each element to initialize:
	 *                 element at index <em>i</em> gets assigned
	 *                 {@code (byte) supplier.applyAsInt(i)}
	 * @throws NegativeArraySizeException if {@code length} is negative
	 */
	public StorageTreeByteArray(int length, IntUnaryOperator supplier) {
		this(length);
	
		IntStream.range(0, length).forEachOrdered(index -> set(index, (byte) supplier.applyAsInt(index)));
	}

	/**
	 * Yields a snapshot of the given array.
	 * 
	 * @param parent the array
	 */
	private StorageTreeByteArray(StorageTreeByteArray parent) {
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
	private abstract static class Node extends Storage {
		protected final int index;
		protected final byte value;
		protected final Node left, right;

		private Node(int index, byte value, Node left, Node right) {
			this.index = index;
			this.value = value;
			this.left = left;
			this.right = right;
		}

		protected static Node mkBlack(int index, byte value, Node left, Node right) {
			return new BlackNode(index, value, left, right);
		}

		protected static Node mkRed(int index, byte value, Node left, Node right) {
			return new RedNode(index, value, left, right);
		}

		protected static Node mkRed(int index, byte value) {
			return new RedNode(index, value, null, null);
		}

		@Override
		public int hashCode() { // unused, but needed to satisfy white-listing for addition of Nodes inside Java collections
			return 42;
		}

		protected abstract Node setValue(byte value);

		protected abstract Node setLeft(Node left);

		protected abstract Node setRight(Node right);

		protected abstract Node rotateRight();

		protected abstract Node rotateLeft();

		protected abstract Node flipColors();

		protected abstract Node flipColor();
	}

	private static class RedNode extends Node {

		private RedNode(int index, byte value, Node left, Node right) {
			super(index, value, left, right);
		}

		@Override
		protected Node flipColor() {
			return mkBlack(index, value, left, right);
		}

		@Override
		protected Node rotateLeft() {
			final Node x = right;
			Node newThis = mkRed(index, value, left, x.left);
			return mkRed(x.index, x.value, newThis, x.right);
		}

		@Override
		protected Node rotateRight() {
			final Node x = left;
			Node newThis = mkRed(index, value, x.right, right);
			return mkRed(x.index, x.value, x.left, newThis);
		}

		@Override
		protected Node setValue(byte value) {
			return mkRed(index, value, left, right);
		}

		@Override
		protected Node setLeft(Node left) {
			return mkRed(index, value, left, right);
		}

		@Override
		protected Node setRight(Node right) {
			return mkRed(index, value, left, right);
		}

		@Override
		protected Node flipColors() {
			return mkBlack(index, value, left.flipColor(), right.flipColor());
		}
	}

	private static class BlackNode extends Node {

		private BlackNode(int index, byte value, Node left, Node right) {
			super(index, value, left, right);
		}

		@Override
		protected Node flipColor() {
			return mkRed(index, value, left, right);
		}

		@Override
		protected Node rotateLeft() {
			final Node x = right;
			Node newThis = mkRed(index, value, left, x.left);
			return mkBlack(x.index, x.value, newThis, x.right);
		}

		@Override
		protected Node rotateRight() {
			final Node x = left;
			Node newThis = mkRed(index, value, x.right, right);
			return mkBlack(x.index, x.value, x.left, newThis);
		}

		@Override
		protected Node setValue(byte value) {
			return mkBlack(index, value, left, right);
		}

		@Override
		protected Node setLeft(Node left) {
			return mkBlack(index, value, left, right);
		}

		@Override
		protected Node setRight(Node right) {
			return mkBlack(index, value, left, right);
		}

		@Override
		protected Node flipColors() {
			return mkRed(index, value, left.flipColor(), right.flipColor());
		}
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
		return x instanceof RedNode;
	}

	/**
	 * Determines if the given node is black.
	 * 
	 * @param x the node
	 * @return true if and only if {@code x} is black
	 */
	private static boolean isBlack(Node x) {
		return x == null || x instanceof BlackNode;
	}

	private static int compareTo(int index1, int index2) {
		return index1 - index2;
	}

	@Override
	public @View byte get(int index) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index + " in get is outside bounds [0," + length + ")");

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
			throw new ArrayIndexOutOfBoundsException(index + " in set is outside bounds [0," + length + ")");

		root = set(root, index, value);
		mkRootBlack();
	}

	// insert the index-value pair in the subtree rooted at h
	private static Node set(Node h, int index, byte value) { 
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
	public void update(int index, IntUnaryOperator how) {
		if (index < 0 || index >= length)
			throw new ArrayIndexOutOfBoundsException(index);

		root = update(root, index, how);
		mkRootBlack();
	}

	private static Node update(Node h, int index, IntUnaryOperator how) { 
		if (h == null) return Node.mkRed(index, (byte) how.applyAsInt(0));

		int cmp = compareTo(index, h.index);
		if      (cmp < 0) h = h.setLeft(update(h.left,  index, how)); 
		else if (cmp > 0) h = h.setRight(update(h.right, index, how)); 
		else              h = h.setValue((byte) how.applyAsInt(h.value));

		// fix-up any right-leaning links
		if (isRed(h.right) && isBlack(h.left))     h = h.rotateLeft();
		if (isRed(h.left)  &&  isRed(h.left.left)) h = h.rotateRight();
		if (isRed(h.left)  &&  isRed(h.right))     h = h.flipColors();

		return h;
	}

	@Override
	public Iterator<Byte> iterator() {
		return new BytesIterator(root, length);
	}

	private static class BytesIterator implements Iterator<Byte> {
		// the path under enumeration; it holds that the left children
		// have already been enumerated
		private final List<Node> stack = new ArrayList<>();
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

	@Override
	public StorageByteArrayView view() {
		
		@Exported
		class StorageByteArrayViewImpl implements StorageByteArrayView {

			@Override
			public Iterator<Byte> iterator() {
				return StorageTreeByteArray.this.iterator();
			}

			@Override
			public int length() {
				return StorageTreeByteArray.this.length();
			}

			@Override
			public byte get(int index) {
				return StorageTreeByteArray.this.get(index);
			}

			@Override
			public IntStream stream() {
				return StorageTreeByteArray.this.stream();
			}

			@Override
			public byte[] toArray() {
				return StorageTreeByteArray.this.toArray();
			}

			@Override
			public StorageByteArrayView snapshot() {
				return StorageTreeByteArray.this.snapshot();
			}
		}

		return new StorageByteArrayViewImpl();
	}

	@Override
	public StorageByteArrayView snapshot() {
		return new StorageTreeByteArray(this).view();
	}
}