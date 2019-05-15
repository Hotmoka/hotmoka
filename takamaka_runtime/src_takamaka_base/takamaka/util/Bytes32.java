package takamaka.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

import takamaka.lang.View;

/**
 * An array of 32 bytes, that can be kept in storage. Unset elements default to 0.
 * The length of the array cannot be changed.
 * By iterating on this object, one gets its values, in increasing index order.
 */

public class Bytes32 extends AbstractByteArray implements ByteArray {

	/**
	 * The immutable size of the array.
	 */
	public final static int length = 32;

	// the elements of the array
	private byte byte0;
	private byte byte1;
	private byte byte2;
	private byte byte3;
	private byte byte4;
	private byte byte5;
	private byte byte6;
	private byte byte7;
	private byte byte8;
	private byte byte9;
	private byte byte10;
	private byte byte11;
	private byte byte12;
	private byte byte13;
	private byte byte14;
	private byte byte15;
	private byte byte16;
	private byte byte17;
	private byte byte18;
	private byte byte19;
	private byte byte20;
	private byte byte21;
	private byte byte22;
	private byte byte23;
	private byte byte24;
	private byte byte25;
	private byte byte26;
	private byte byte27;
	private byte byte28;
	private byte byte29;
	private byte byte30;
	private byte byte31;

	/**
	 * Builds an empty array.
	 */
	public Bytes32() {}

	/**
	 * Builds an empty array with the given elements.
	 * 
	 * @param elements the elements
	 */
	public Bytes32(byte[] elements) {
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
		case 2: return byte2;
		case 3: return byte3;
		case 4: return byte4;
		case 5: return byte5;
		case 6: return byte6;
		case 7: return byte7;
		case 8: return byte8;
		case 9: return byte9;
		case 10: return byte10;
		case 11: return byte11;
		case 12: return byte12;
		case 13: return byte13;
		case 14: return byte14;
		case 15: return byte15;
		case 16: return byte16;
		case 17: return byte17;
		case 18: return byte18;
		case 19: return byte19;
		case 20: return byte20;
		case 21: return byte21;
		case 22: return byte22;
		case 23: return byte23;
		case 24: return byte24;
		case 25: return byte25;
		case 26: return byte26;
		case 27: return byte27;
		case 28: return byte28;
		case 29: return byte29;
		case 30: return byte30;
		case 31: return byte31;
		default: throw new ArrayIndexOutOfBoundsException(index);
		}
	}

	@Override
	public void set(int index, byte value) {
		switch (index) {
		case 0: byte0 = value; return;
		case 1: byte1 = value; return;
		case 2: byte2 = value; return;
		case 3: byte3 = value; return;
		case 4: byte4 = value; return;
		case 5: byte5 = value; return;
		case 6: byte6 = value; return;
		case 7: byte7 = value; return;
		case 8: byte8 = value; return;
		case 9: byte9 = value; return;
		case 10: byte10 = value; return;
		case 11: byte11 = value; return;
		case 12: byte12 = value; return;
		case 13: byte13 = value; return;
		case 14: byte14 = value; return;
		case 15: byte15 = value; return;
		case 16: byte16 = value; return;
		case 17: byte17 = value; return;
		case 18: byte18 = value; return;
		case 19: byte19 = value; return;
		case 20: byte20 = value; return;
		case 21: byte21 = value; return;
		case 22: byte22 = value; return;
		case 23: byte23 = value; return;
		case 24: byte24 = value; return;
		case 25: byte25 = value; return;
		case 26: byte26 = value; return;
		case 27: byte27 = value; return;
		case 28: byte28 = value; return;
		case 29: byte29 = value; return;
		case 30: byte30 = value; return;
		case 31: byte31 = value; return;
		default: throw new ArrayIndexOutOfBoundsException(index);
		}
	}

	@Override
	public void update(int index, IntUnaryOperator how) {
		switch (index) {
		case 0: byte0 = (byte) how.applyAsInt(byte0); return;
		case 1: byte1 = (byte) how.applyAsInt(byte1); return;
		case 2: byte2 = (byte) how.applyAsInt(byte2); return;
		case 3: byte3 = (byte) how.applyAsInt(byte3); return;
		case 4: byte4 = (byte) how.applyAsInt(byte4); return;
		case 5: byte5 = (byte) how.applyAsInt(byte5); return;
		case 6: byte6 = (byte) how.applyAsInt(byte6); return;
		case 7: byte7 = (byte) how.applyAsInt(byte7); return;
		case 8: byte8 = (byte) how.applyAsInt(byte8); return;
		case 9: byte9 = (byte) how.applyAsInt(byte9); return;
		case 10: byte10 = (byte) how.applyAsInt(byte10); return;
		case 11: byte11 = (byte) how.applyAsInt(byte11); return;
		case 12: byte12 = (byte) how.applyAsInt(byte12); return;
		case 13: byte13 = (byte) how.applyAsInt(byte13); return;
		case 14: byte14 = (byte) how.applyAsInt(byte14); return;
		case 15: byte15 = (byte) how.applyAsInt(byte15); return;
		case 16: byte16 = (byte) how.applyAsInt(byte16); return;
		case 17: byte17 = (byte) how.applyAsInt(byte17); return;
		case 18: byte18 = (byte) how.applyAsInt(byte18); return;
		case 19: byte19 = (byte) how.applyAsInt(byte19); return;
		case 20: byte20 = (byte) how.applyAsInt(byte20); return;
		case 21: byte21 = (byte) how.applyAsInt(byte21); return;
		case 22: byte22 = (byte) how.applyAsInt(byte22); return;
		case 23: byte23 = (byte) how.applyAsInt(byte23); return;
		case 24: byte24 = (byte) how.applyAsInt(byte24); return;
		case 25: byte25 = (byte) how.applyAsInt(byte25); return;
		case 26: byte26 = (byte) how.applyAsInt(byte26); return;
		case 27: byte27 = (byte) how.applyAsInt(byte27); return;
		case 28: byte28 = (byte) how.applyAsInt(byte28); return;
		case 29: byte29 = (byte) how.applyAsInt(byte29); return;
		case 30: byte30 = (byte) how.applyAsInt(byte30); return;
		case 31: byte31 = (byte) how.applyAsInt(byte31); return;
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
			case 2: return (Byte) byte2;
			case 3: return (Byte) byte3;
			case 4: return (Byte) byte4;
			case 5: return (Byte) byte5;
			case 6: return (Byte) byte6;
			case 7: return (Byte) byte7;
			case 8: return (Byte) byte8;
			case 9: return (Byte) byte9;
			case 10: return (Byte) byte10;
			case 11: return (Byte) byte11;
			case 12: return (Byte) byte12;
			case 13: return (Byte) byte13;
			case 14: return (Byte) byte14;
			case 15: return (Byte) byte15;
			case 16: return (Byte) byte16;
			case 17: return (Byte) byte17;
			case 18: return (Byte) byte18;
			case 19: return (Byte) byte19;
			case 20: return (Byte) byte20;
			case 21: return (Byte) byte21;
			case 22: return (Byte) byte22;
			case 23: return (Byte) byte23;
			case 24: return (Byte) byte24;
			case 25: return (Byte) byte25;
			case 26: return (Byte) byte26;
			case 27: return (Byte) byte27;
			case 28: return (Byte) byte28;
			case 29: return (Byte) byte29;
			case 30: return (Byte) byte30;
			case 31: return (Byte) byte31;
			default: throw new NoSuchElementException();
			}
		}
	}

	@Override
	public IntStream stream() {
		return IntStream.of(byte0, byte1, byte2, byte3, byte4, byte5, byte6, byte7, byte8, byte9, byte10, byte11, byte12, byte13, byte14, byte15,
							byte16, byte17, byte18, byte19, byte20, byte21, byte22, byte23, byte24, byte25, byte26, byte27, byte28, byte29, byte30, byte31);
	}

	@Override
	public byte[] toArray() {
		return new byte[] { byte0, byte1, byte2, byte3, byte4, byte5, byte6, byte7, byte8, byte9, byte10, byte11, byte12, byte13, byte14, byte15,
							byte16, byte17, byte18, byte19, byte20, byte21, byte22, byte23, byte24, byte25, byte26, byte27, byte28, byte29, byte30, byte31 };
	}
}