package io.hotmoka.xodus.env;

import io.hotmoka.xodus.ByteIterable;

public class Store {
	private final jetbrains.exodus.env.Store parent;

	private Store(jetbrains.exodus.env.Store parent) {
		this.parent = parent;
	}

	public static Store fromNative(jetbrains.exodus.env.Store parent) {
		return new Store(parent);
	}

	public void put(Transaction txn, ByteIterable key, ByteIterable value) {
		parent.put(txn.toNative(), key.toNative(), value.toNative());
	}

	public ByteIterable get(Transaction txn, ByteIterable key) {
		return ByteIterable.fromNative(parent.get(txn.toNative(), key.toNative()));
	}
}