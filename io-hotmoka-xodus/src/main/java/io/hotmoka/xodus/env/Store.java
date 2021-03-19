package io.hotmoka.xodus.env;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.xodus.ByteIterable;

public class Store {
	protected final static Logger logger = LoggerFactory.getLogger(Store.class);
	private final jetbrains.exodus.env.Store parent;

	Store(jetbrains.exodus.env.Store parent) {
		this.parent = parent;
	}

	public void put(Transaction txn, ByteIterable key, ByteIterable value) {
		if (!parent.put(txn.toNative(), key.toNative(), value.toNative()))
			logger.error("couldn't write key " + key + " into the Xodus store");
	}

	public ByteIterable get(Transaction txn, ByteIterable key) {
		return ByteIterable.fromNative(parent.get(txn.toNative(), key.toNative()));
	}

	public void remove(Transaction txn, ByteIterable key) {
		if (!parent.delete(txn.toNative(), key.toNative()))
			logger.error("couldn't delete key " + key + " from the Xodus store");
	}
}