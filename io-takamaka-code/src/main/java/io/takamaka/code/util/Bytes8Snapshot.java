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
 * An immutable array of 8 bytes, that can be kept in storage. Unset elements default to 0.
 * The length of the array cannot be changed. Its elements cannot be updated.
 * By iterating on this object, one gets its values, in increasing index order.
 */

@Immutable @Exported
public final class Bytes8Snapshot extends AbstractStorageByteArrayView {

	/**
	 * The immutable size of the array.
	 */
	public final static int length = 8;

	// the elements of the array
	private final byte byte0;
	private final byte byte1;
	private final byte byte2;
	private final byte byte3;
	private final byte byte4;
	private final byte byte5;
	private final byte byte6;
	private final byte byte7;

	/**
	 * Builds an array whose elements
	 * are all initialized to the given value.
	 * 
	 * @param initialValue the initial value of the array
	 */
	public Bytes8Snapshot(byte initialValue) {
		this(initialValue, initialValue, initialValue, initialValue, initialValue, initialValue, initialValue, initialValue);
	}

	/**
	 * Builds an array whose elements
	 * are all initialized to the value provided by the given supplier.
	 * 
	 * @param supplier the supplier of the initial values of the array. It gets
	 *                 used repeatedly for each element to initialize. Its result
	 *                 is cast to {@code byte}
	 */
	public Bytes8Snapshot(IntSupplier supplier) {
		this((byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(), (byte) supplier.getAsInt(),
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
	public Bytes8Snapshot(IntUnaryOperator supplier) {
		this((byte) supplier.applyAsInt(0), (byte) supplier.applyAsInt(1), (byte) supplier.applyAsInt(2), (byte) supplier.applyAsInt(3),
			(byte) supplier.applyAsInt(4), (byte) supplier.applyAsInt(5), (byte) supplier.applyAsInt(6), (byte) supplier.applyAsInt(7));
	}

	/**
	 * Builds an array with the given elements. The resulting
	 * object is not backed by the elements, meaning that subsequent
	 * updates to the array of elements does not affect the created object.
	 * 
	 * @param elements the elements
	 */
	public Bytes8Snapshot(byte[] elements) {
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
	}

	/**
	 * Builds an array with the given elements.
	 */
	public Bytes8Snapshot(byte byte0, byte byte1, byte byte2, byte byte3, byte byte4, byte byte5, byte byte6, byte byte7) {
		this.byte0 = byte0;
		this.byte1 = byte1;
		this.byte2 = byte2;
		this.byte3 = byte3;
		this.byte4 = byte4;
		this.byte5 = byte5;
		this.byte6 = byte6;
		this.byte7 = byte7;
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
			default: {
				nextKey = length;
				throw new NoSuchElementException();
			}
			}
		}
	}

	@Override @View
	public boolean equals(Object other) {
		if (other instanceof Bytes8Snapshot) {
			// optimized for special case
			Bytes8Snapshot otherAsBytes = (Bytes8Snapshot) other;
			return byte0 == otherAsBytes.byte0 &&
				byte1 == otherAsBytes.byte1 &&
				byte2 == otherAsBytes.byte2 &&
				byte3 == otherAsBytes.byte3 &&
				byte4 == otherAsBytes.byte4 &&
				byte5 == otherAsBytes.byte5 &&
				byte6 == otherAsBytes.byte6 &&
				byte7 == otherAsBytes.byte7;
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
			(byte7 << 7);
	}

	@Override
	public IntStream stream() {
		return IntStream.of(byte0, byte1, byte2, byte3, byte4, byte5, byte6, byte7);
	}

	@Override
	public byte[] toArray() {
		return new byte[] { byte0, byte1, byte2, byte3, byte4, byte5, byte6, byte7 };
	}

	@Override
	public StorageByteArrayView snapshot() {
		return this;
	}
}