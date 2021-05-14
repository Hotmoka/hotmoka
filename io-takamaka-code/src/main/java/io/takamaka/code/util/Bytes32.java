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
import io.takamaka.code.lang.View;

/**
 * A mutable array of 32 bytes, that can be kept in storage. Unset elements default to 0.
 * The length of the array cannot be changed but its elements can be updated.
 * By iterating on this object, one gets its values, in increasing index order.
 */

public class Bytes32 extends AbstractStorageByteArrayView implements StorageByteArray {

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
	 * Builds an empty array of the given length. Its elements are
	 * initialized to 0.
	 */
	public Bytes32() {}

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

		for (int pos = 0; pos < length; pos++)
			set(pos, elements[pos]);
	}

	/**
	 * Builds an array with the given elements.
	 */
	public Bytes32(byte byte0, byte byte1, byte byte2, byte byte3, byte byte4, byte byte5, byte byte6, byte byte7,
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

	/**
	 * Builds an array whose elements
	 * are all initialized to the given value.
	 * 
	 * @param initialValue the initial value of the array
	 */
	public Bytes32(byte initialValue) {
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
	public Bytes32(IntSupplier supplier) {
		IntStream.range(0, 32).forEachOrdered(index -> set(index, (byte) supplier.getAsInt()));
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
	public Bytes32(IntUnaryOperator supplier) {
		IntStream.range(0, length).forEachOrdered(index -> set(index, (byte) supplier.applyAsInt(index)));
	}

	@Override
	public @View int length() {
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
			default: throw new NoSuchElementException();
			}
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Bytes32) {
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

	@Override
	public int hashCode() {
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
	public StorageByteArrayView view() {

		@Exported
		class StorageByteArrayViewImpl implements StorageByteArrayView {

			@Override
			public Iterator<Byte> iterator() {
				return Bytes32.this.iterator();
			}

			@Override
			public int length() {
				return Bytes32.this.length();
			}

			@Override
			public byte get(int index) {
				return Bytes32.this.get(index);
			}

			@Override
			public IntStream stream() {
				return Bytes32.this.stream();
			}

			@Override
			public byte[] toArray() {
				return Bytes32.this.toArray();
			}

			@Override
			public StorageByteArrayView snapshot() {
				return Bytes32.this.snapshot();
			}
		}

		return new StorageByteArrayViewImpl();
	}

	@Override
	public StorageByteArrayView snapshot() {
		return new Bytes32Snapshot(byte0, byte1, byte2, byte3, byte4, byte5, byte6, byte7, byte8, byte9, byte10, byte11, byte12, byte13, byte14, byte15,
			byte16, byte17, byte18, byte19, byte20, byte21, byte22, byte23, byte24, byte25, byte26, byte27, byte28, byte29, byte30, byte31);
	}
}