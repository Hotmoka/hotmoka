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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.Immutable;
import io.takamaka.code.lang.View;

/**
 * An immutable array of 32 bytes, that can be kept in storage. Unset elements default to 0.
 * The length of the array cannot be changed. Its elements cannot be updated.
 * By iterating on this object, one gets its values, in increasing index order.
 */

@Immutable @Exported
public final class Bytes32Snapshot extends AbstractStorageByteArrayView {

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
	 * Builds an array whose elements
	 * are all initialized to the given value.
	 * 
	 * @param initialValue the initial value of the array
	 */
	public Bytes32Snapshot(byte initialValue) {
		this(initialValue, initialValue, initialValue, initialValue, initialValue, initialValue, initialValue, initialValue,
			initialValue, initialValue, initialValue, initialValue, initialValue, initialValue, initialValue, initialValue,
			initialValue, initialValue, initialValue, initialValue, initialValue, initialValue, initialValue, initialValue,
			initialValue, initialValue, initialValue, initialValue, initialValue, initialValue, initialValue, initialValue);
	}

	/**
	 * Builds an array whose elements
	 * are all initialized to the value provided by the given supplier.
	 * 
	 * @param supplier the supplier of the initial values of the array. It gets
	 *                 used repeatedly for each element to initialize. Its result
	 *                 is cast to {@code byte}
	 */
	public Bytes32Snapshot(IntSupplier supplier) {
		this((byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(),
			(byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(),
			(byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(),
			(byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(),
			(byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(),
			(byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(),
			(byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(),
			(byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt());
	}

	/**
	 * Builds an array whose elements
	 * are all initialized to the value provided by the given supplier.
	 * 
	 * @param supplier the supplier of the initial values of the array. It gets
	 *                 used repeatedly for each element to initialize:
	 *                 element at index <em>i</em> gets assigned
	 *                 {@code (byte) supplier.applyAsInt(i)}
	 */
	public Bytes32Snapshot(IntUnaryOperator supplier) {
		this((byte) supplier.applyAsInt(0), (byte) supplier.applyAsInt(1), (byte) supplier.applyAsInt(2), (byte) supplier.applyAsInt(3),
			(byte) supplier.applyAsInt(4), (byte) supplier.applyAsInt(5), (byte) supplier.applyAsInt(6), (byte) supplier.applyAsInt(7),
			(byte) supplier.applyAsInt(8), (byte) supplier.applyAsInt(9), (byte) supplier.applyAsInt(10), (byte) supplier.applyAsInt(11),
			(byte) supplier.applyAsInt(12), (byte) supplier.applyAsInt(13), (byte) supplier.applyAsInt(14), (byte) supplier.applyAsInt(15),
			(byte) supplier.applyAsInt(16), (byte) supplier.applyAsInt(17), (byte) supplier.applyAsInt(18), (byte) supplier.applyAsInt(19),
			(byte) supplier.applyAsInt(20), (byte) supplier.applyAsInt(21), (byte) supplier.applyAsInt(22), (byte) supplier.applyAsInt(23),
			(byte) supplier.applyAsInt(24), (byte) supplier.applyAsInt(25), (byte) supplier.applyAsInt(26), (byte) supplier.applyAsInt(27),
			(byte) supplier.applyAsInt(28), (byte) supplier.applyAsInt(29), (byte) supplier.applyAsInt(30), (byte) supplier.applyAsInt(31));
	}

	/**
	 * Builds an array with the given elements. The resulting
	 * object is not backed by the elements, meaning that subsequent
	 * updates to the array of elements does not affect the created object.
	 * 
	 * @param elements the elements
	 */
	public Bytes32Snapshot(byte[] elements) {
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

	/**
	 * Builds an array with the given elements.
	 * 
	 * @param byte0 the byte number 0
	 * @param byte1 the byte number 1
	 * @param byte2 the byte number 2
	 * @param byte3 the byte number 3
	 * @param byte4 the byte number 4
	 * @param byte5 the byte number 5
	 * @param byte6 the byte number 6
	 * @param byte7 the byte number 7
	 * @param byte8 the byte number 8
	 * @param byte9 the byte number 9
	 * @param byte10 the byte number 10
	 * @param byte11 the byte number 11
	 * @param byte12 the byte number 12
	 * @param byte13 the byte number 13
	 * @param byte14 the byte number 14
	 * @param byte15 the byte number 15
	 * @param byte16 the byte number 16
	 * @param byte17 the byte number 17
	 * @param byte18 the byte number 18
	 * @param byte19 the byte number 19
	 * @param byte20 the byte number 20
	 * @param byte21 the byte number 21
	 * @param byte22 the byte number 22
	 * @param byte23 the byte number 23
	 * @param byte24 the byte number 24
	 * @param byte25 the byte number 25
	 * @param byte26 the byte number 26
	 * @param byte27 the byte number 27
	 * @param byte28 the byte number 28
	 * @param byte29 the byte number 29
	 * @param byte30 the byte number 30
	 * @param byte31 the byte number 31
	 */
	public Bytes32Snapshot(byte byte0, byte byte1, byte byte2, byte byte3, byte byte4, byte byte5, byte byte6, byte byte7,
			byte byte8, byte byte9, byte byte10, byte byte11, byte byte12, byte byte13, byte byte14, byte byte15, byte byte16,
			byte byte17, byte byte18, byte byte19, byte byte20, byte byte21, byte byte22, byte byte23, byte byte24,
			byte byte25, byte byte26, byte byte27, byte byte28, byte byte29, byte byte30, byte byte31) {

		this.byte0 = byte0;
		this.byte1 = byte1;
		this.byte2 = byte2;
		this.byte3 = byte3;
		this.byte4 = byte4;
		this.byte5 = byte5;
		this.byte6 = byte6;
		this.byte7 = byte7;
		this.byte8 = byte8;
		this.byte9 = byte9;
		this.byte10 = byte10;
		this.byte11 = byte11;
		this.byte12 = byte12;
		this.byte13 = byte13;
		this.byte14 = byte14;
		this.byte15 = byte15;
		this.byte16 = byte16;
		this.byte17 = byte17;
		this.byte18 = byte18;
		this.byte19 = byte19;
		this.byte20 = byte20;
		this.byte21 = byte21;
		this.byte22 = byte22;
		this.byte23 = byte23;
		this.byte24 = byte24;
		this.byte25 = byte25;
		this.byte26 = byte26;
		this.byte27 = byte27;
		this.byte28 = byte28;
		this.byte29 = byte29;
		this.byte30 = byte30;
		this.byte31 = byte31;
	}

	@Override @View
	public int length() {
		return length;
	}

	@Override @View
	public byte get(int index) {
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
			default: {
				nextKey = length;
				throw new NoSuchElementException();
			}
			}
		}
	}

	@Override @View
	public boolean equals(Object other) {
		if (other instanceof Bytes32Snapshot) {
			// optimized for special case
			Bytes32Snapshot otherAsBytes = (Bytes32Snapshot) other;
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

	@Override
	public StorageByteArrayView snapshot() {
		return this;
	}
}