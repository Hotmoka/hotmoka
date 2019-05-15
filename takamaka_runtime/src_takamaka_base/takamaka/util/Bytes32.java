package takamaka.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

import takamaka.lang.Immutable;
import takamaka.lang.View;

/**
 * An immutable array of 32 bytes, that can be kept in storage. Unset elements default to 0.
 * The length of the array cannot be changed. Its elements cannot be updated.
 * By iterating on this object, one gets its values, in increasing index order.
 */

@Immutable
public final class Bytes32 extends AbstractByteArray {

	/**
	 * The immutable size of the array.
	 */
	public final static int length = 32;

	// the elements of the array
	private final byte byte0;
	private final byte byte1;
	private final byte byte2;
	private final byte byte3;
	private final byte byte4;
	private final byte byte5;
	private final byte byte6;
	private final byte byte7;
	private final byte byte8;
	private final byte byte9;
	private final byte byte10;
	private final byte byte11;
	private final byte byte12;
	private final byte byte13;
	private final byte byte14;
	private final byte byte15;
	private final byte byte16;
	private final byte byte17;
	private final byte byte18;
	private final byte byte19;
	private final byte byte20;
	private final byte byte21;
	private final byte byte22;
	private final byte byte23;
	private final byte byte24;
	private final byte byte25;
	private final byte byte26;
	private final byte byte27;
	private final byte byte28;
	private final byte byte29;
	private final byte byte30;
	private final byte byte31;

	/**
	 * Builds an array with the given elements. The resulting
	 * object is not backed by the elements, meaning that subsequent
	 * updates to the array of elements does not affect the created object.
	 * 
	 * @param elements the elements
	 */
	public Bytes32(byte[] elements) {
		if (elements == null)
			throw new IllegalArgumentException("Expected a non-null array of elements");
		if (elements.length != length)
			throw new IllegalArgumentException("Expected " + length + " elements, but got " + elements.length);

		byte0 = elements[0];
		byte1 = elements[1];
		byte2 = elements[2];
		byte3 = elements[3];
		byte4 = elements[4];
		byte5 = elements[5];
		byte6 = elements[6];
		byte7 = elements[7];
		byte8 = elements[8];
		byte9 = elements[9];
		byte10 = elements[10];
		byte11 = elements[11];
		byte12 = elements[12];
		byte13 = elements[13];
		byte14 = elements[14];
		byte15 = elements[15];
		byte16 = elements[16];
		byte17 = elements[17];
		byte18 = elements[18];
		byte19 = elements[19];
		byte20 = elements[20];
		byte21 = elements[21];
		byte22 = elements[22];
		byte23 = elements[23];
		byte24 = elements[24];
		byte25 = elements[25];
		byte26 = elements[26];
		byte27 = elements[27];
		byte28 = elements[28];
		byte29 = elements[29];
		byte30 = elements[30];
		byte31 = elements[31];
	}

	@Override @View
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

	@Override @View
	public boolean equals(Object other) {
		if (other instanceof Bytes32) {
			// optimized for special case
			Bytes32 otherAsBytes = (Bytes32) other;
			return byte0 == otherAsBytes.byte0 &&
				byte1 == otherAsBytes.byte1 &&
				byte2 == otherAsBytes.byte2 &&
				byte3 == otherAsBytes.byte3 &&
				byte4 == otherAsBytes.byte4 &&
				byte5 == otherAsBytes.byte5 &&
				byte6 == otherAsBytes.byte6 &&
				byte7 == otherAsBytes.byte7 &&
				byte8 == otherAsBytes.byte8 &&
				byte9 == otherAsBytes.byte9 &&
				byte10 == otherAsBytes.byte10 &&
				byte11 == otherAsBytes.byte11 &&
				byte12 == otherAsBytes.byte12 &&
				byte13 == otherAsBytes.byte13 &&
				byte14 == otherAsBytes.byte14 &&
				byte15 == otherAsBytes.byte15 &&
				byte16 == otherAsBytes.byte16 &&
				byte17 == otherAsBytes.byte17 &&
				byte18 == otherAsBytes.byte18 &&
				byte19 == otherAsBytes.byte19 &&
				byte20 == otherAsBytes.byte20 &&
				byte21 == otherAsBytes.byte21 &&
				byte22 == otherAsBytes.byte22 &&
				byte23 == otherAsBytes.byte23 &&
				byte24 == otherAsBytes.byte24 &&
				byte25 == otherAsBytes.byte25 &&
				byte26 == otherAsBytes.byte26 &&
				byte27 == otherAsBytes.byte27 &&
				byte28 == otherAsBytes.byte28 &&
				byte29 == otherAsBytes.byte29 &&
				byte30 == otherAsBytes.byte30 &&
				byte31 == otherAsBytes.byte31;
		}
		else
			return super.equals(other);
	}

	@Override @View
	public int hashCode() {
		// optimized
		return byte0 ^
			(byte1 << 1) ^
			(byte2 << 2) ^
			(byte3 << 3) ^
			(byte4 << 4) ^
			(byte5 << 5) ^
			(byte6 << 6) ^
			(byte7 << 7) ^
			(byte8 << 8) ^
			(byte9 << 9) ^
			(byte10 << 10) ^
			(byte11 << 11) ^
			(byte12 << 12) ^
			(byte13 << 13) ^
			(byte14 << 14) ^
			(byte15 << 15) ^
			(byte16 << 16) ^
			(byte17 << 17) ^
			(byte18 << 18) ^
			(byte19 << 19) ^
			(byte20 << 20) ^
			(byte21 << 21) ^
			(byte22 << 22) ^
			(byte23 << 23) ^
			byte24 ^
			(byte25 << 1) ^
			(byte26 << 2) ^
			(byte27 << 3) ^
			(byte28 << 4) ^
			(byte29 << 5) ^
			(byte30 << 6) ^
			(byte31 << 7);
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