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
}