package io.hotmoka.xodus.env;

import io.hotmoka.xodus.ByteIterable;

public class Store {
	private final jetbrains.exodus.env.Store parent;

	/**
	 * Used to lock concurrent accesses to the underlying Xodus database.
	 */
	private final Object lock = new Object();

	Store(jetbrains.exodus.env.Store parent) {
		this.parent = parent;
	}

	public void put(Transaction txn, ByteIterable key, ByteIterable value) {
		jetbrains.exodus.env.Transaction txnToNative = txn.toNative();
		jetbrains.exodus.ByteIterable keyToNative = key.toNative();
		jetbrains.exodus.ByteIterable valueToNative = value.toNative();

		synchronized (lock) {
			parent.put(txnToNative, keyToNative, valueToNative);
		}
	}

	public ByteIterable get(Transaction txn, ByteIterable key) {
		jetbrains.exodus.env.Transaction txnToNative = txn.toNative();
		jetbrains.exodus.ByteIterable keyToNative = key.toNative();
		jetbrains.exodus.ByteIterable result;

		synchronized (lock) {
			result = parent.get(txnToNative, keyToNative);
		}

		return ByteIterable.fromNative(result);
	}
}