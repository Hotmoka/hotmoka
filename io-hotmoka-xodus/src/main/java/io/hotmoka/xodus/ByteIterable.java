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

package io.hotmoka.xodus;

import jetbrains.exodus.ArrayByteIterable;

/**
 * This is an adapter for the Xodus {@code ByteIterable} class.
 * It is a mix of iterable and array of bytes. It allows to lazily enumerate bytes without boxing.
 * On the other hand, you can get its length using method {@link #getLength()}.
 */
public class ByteIterable {

	/**
	 * The adapted parent.
	 */
	private final jetbrains.exodus.ByteIterable parent;

	private ByteIterable(jetbrains.exodus.ByteIterable parent) {
		this.parent = parent;
	}

	/**
	 * Adapts a Xodus byte iterable.
	 * 
	 * @param parent the Xodus byte iterable to adapt
	 * @return the adapted byte iterable
	 */
	public static ByteIterable fromNative(jetbrains.exodus.ByteIterable parent) {
		return parent == null ? null : new ByteIterable(parent);
	}

	/**
	 * Yields a byte iterable containing only the given byte.
	 * 
	 * @param b the byte
	 * @return the resulting byte iterable
	 */
	public static ByteIterable fromByte(byte b) {
		return new ByteIterable(ArrayByteIterable.fromByte(b));
	}

	/**
	 * Yields a byte iterable containing only the bytes in the given array.
	 * 
	 * @param bs the array of bytes
	 * @return the resulting byte iterable
	 */
	public static ByteIterable fromBytes(byte[] bs) {
		return new ByteIterable(new ArrayByteIterable(bs));
	}

	/**
	 * Yields the Xodus byte iterable corresponding to this object.
	 * 
	 * @return the Xodus byte iterable corresponding to this object
	 */
	public jetbrains.exodus.ByteIterable toNative() {
		return parent;
	}

	/**
	 * Yields the bytes inside this byte iterable.
	 * 
	 * @return the bytes inside this byte iterable
	 */
	public byte[] getBytes() {
		return parent.getBytesUnsafe();
	}

	/**
	 * Yields the length (number of bytes) of this byte iterable.
	 * 
	 * @return the length of this byte iterable
	 */
	public int getLength() {
		return parent.getLength();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ByteIterable bi && parent.equals(bi.parent);
	}

	@Override
	public int hashCode() {
		return parent.hashCode();
	}

	@Override
	public String toString() {
		return parent.toString();
	}
}