package takamaka.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

import takamaka.lang.View;

/**
 * An array of 2 bytes, that can be kept in storage. Unset elements default to 0.
 * The length of the array cannot be changed.
 * By iterating on this object, one gets its values, in increasing index order.
 */

public class Bytes2 extends AbstractByteArray implements ByteArray {

	/**
	 * The immutable size of the array.
	 */
	public final static int length = 2;

	// the elements of the array
	private byte byte0;
	private byte byte1;

	/**
	 * Builds an empty array.
	 */
	public Bytes2() {
	}

	/**
	 * Builds an empty array with the given elements.
	 * 
	 * @param elements the elements
	 */
	public Bytes2(byte[] elements) {
		if (elements.length != length)
			throw new IllegalArgumentException("Expected " + length + " elements");

		for (int pos = 0; pos < length; pos++)
			set(pos, elements[pos]);
	}

	@Override
	public int length() {
		return length;
	}

	@Override
	public @View byte get(int index) {
		switch (index) {
		case 0: return byte0;
		case 1: return byte1;
		default: throw new ArrayIndexOutOfBoundsException(index);
		}
	}

	@Override
	public void set(int index, byte value) {
		switch (index) {
		case 0: byte0 = value; return;
		case 1: byte1 = value; return;
		default: throw new ArrayIndexOutOfBoundsException(index);
		}
	}

	@Override
	public void update(int index, IntUnaryOperator how) {
		switch (index) {
		case 0: byte0 = (byte) how.applyAsInt(byte0); return;
		case 1: byte1 = (byte) how.applyAsInt(byte1); return;
		default: throw new ArrayIndexOutOfBoundsException(index);
		}
	}

	@Override
	public Iterator<Byte> iterator() {
		return new BytesIterator();
	}

	private class BytesIterator implements Iterator<Byte> {
		private int nextKey;

		@Override
		public boolean hasNext() {
			return nextKey < length;
		}

		@Override
		public Byte next() {
			switch (nextKey++) {
			case 0: return (Byte) byte0;
			case 1: return (Byte) byte1;
			default: throw new NoSuchElementException();
			}
		}
	}

	@Override
	public IntStream stream() {
		return IntStream.of(byte0, byte1);
	}

	@Override
	public byte[] toArray() {
		return new byte[] { byte0, byte1 };
	}
}