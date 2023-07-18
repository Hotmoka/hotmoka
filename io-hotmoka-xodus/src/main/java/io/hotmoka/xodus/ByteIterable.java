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

public class ByteIterable {
	private final jetbrains.exodus.ByteIterable parent;

	private ByteIterable(jetbrains.exodus.ByteIterable parent) {
		this.parent = parent;
	}

	public static ByteIterable fromNative(jetbrains.exodus.ByteIterable parent) {
		if (parent == null)
			return null;
		else
			return new ByteIterable(parent);
	}

	public static ByteIterable fromByte(byte b) {
		return new ByteIterable(ArrayByteIterable.fromByte(b));
	}

	public static ByteIterable fromBytes(byte[] bs) {
		return new ByteIterable(new ArrayByteIterable(bs));
	}

	public jetbrains.exodus.ByteIterable toNative() {
		return parent;
	}

	public byte[] getBytes() {
		return parent.getBytesUnsafe();
	}

	public int getLength() {
		return parent.getLength();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ByteIterable && parent.equals(((ByteIterable) other).parent);
	}

	@Override
	public int hashCode() {
		return parent.hashCode();
	}
}