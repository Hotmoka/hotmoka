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
 * An immutable array of 16 bytes, that can be kept in storage. Unset elements default to 0.
 * The length of the array cannot be changed. Its elements cannot be updated.
 * By iterating on this object, one gets its values, in increasing index order.
 */

@Immutable @Exported
public final class Bytes16Snapshot extends AbstractStorageByteArrayView {

	/**
	 * The immutable size of the array.
	 */
	public final static int length = 16;

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

	/**
	 * Builds an array whose elements
	 * are all initialized to the given value.
	 * 
	 * @param initialValue the initial value of the array
	 */
	public Bytes16Snapshot(byte initialValue) {
		this(initialValue, initialValue, initialValue, initialValue, initialValue, initialValue, initialValue, initialValue,
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
	public Bytes16Snapshot(IntSupplier supplier) {
		this((byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(),
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
	public Bytes16Snapshot(IntUnaryOperator supplier) {
		this((byte) supplier.applyAsInt(0), (byte) supplier.applyAsInt(1), (byte) supplier.applyAsInt(2), (byte) supplier.applyAsInt(3),
			(byte) supplier.applyAsInt(4), (byte) supplier.applyAsInt(5), (byte) supplier.applyAsInt(6), (byte) supplier.applyAsInt(7),
			(byte) supplier.applyAsInt(8), (byte) supplier.applyAsInt(9), (byte) supplier.applyAsInt(10), (byte) supplier.applyAsInt(11),
			(byte) supplier.applyAsInt(12), (byte) supplier.applyAsInt(13), (byte) supplier.applyAsInt(14), (byte) supplier.applyAsInt(15));
	}

	/**
	 * Builds an array with the given elements. The resulting
	 * object is not backed by the elements, meaning that subsequent
	 * updates to the array of elements does not affect the created object.
	 * 
	 * @param elements the elements
	 */
	public Bytes16Snapshot(byte[] elements) {
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
	 */
	public Bytes16Snapshot(byte byte0, byte byte1, byte byte2, byte byte3, byte byte4, byte byte5, byte byte6, byte byte7,
			byte byte8, byte byte9, byte byte10, byte byte11, byte byte12, byte byte13, byte byte14, byte byte15) {

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
			default: {
				nextKey = length;
				throw new NoSuchElementException();
			}
			}
		}
	}

	@Override @View
	public boolean equals(Object other) {
		if (other instanceof Bytes16Snapshot) {
			// optimized for special case
			Bytes16Snapshot otherAsBytes = (Bytes16Snapshot) other;
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
				byte15 == otherAsBytes.byte15;
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
			(byte15 << 15);
	}

	@Override
	public IntStream stream() {
		return IntStream.of(byte0, byte1, byte2, byte3, byte4, byte5, byte6, byte7, byte8, byte9, byte10, byte11, byte12, byte13, byte14, byte15);
	}

	@Override
	public byte[] toArray() {
		return new byte[] { byte0, byte1, byte2, byte3, byte4, byte5, byte6, byte7, byte8, byte9, byte10, byte11, byte12, byte13, byte14, byte15 };
	}

	@Override
	public StorageByteArrayView snapshot() {
		return this;
	}
}