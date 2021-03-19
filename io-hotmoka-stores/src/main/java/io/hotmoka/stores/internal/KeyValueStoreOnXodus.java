package io.hotmoka.stores.internal;

import java.util.NoSuchElementException;

import io.hotmoka.patricia.KeyValueStore;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;

/**
 * A key/value store implemented over the Xodus database.
 */
class KeyValueStoreOnXodus implements KeyValueStore {
	private final Store store;
	private final Transaction txn;
	private byte[] root;

	KeyValueStoreOnXodus(Store store, Transaction txn, byte[] root) {
		this.store = store;
		this.txn = txn;
		this.root = root;
	}

    @Override
	public byte[] getRoot() {
    	return root;
	}

	@Override
	public void setRoot(byte[] root) {
		this.root = root;
	}

	@Override
	public void put(byte[] key, byte[] value) {
		store.put(txn, ByteIterable.fromBytes(key), ByteIterable.fromBytes(value));
	}

	@Override
	public void remove(byte[] key) {
		store.remove(txn, ByteIterable.fromBytes(key));
	}

	@Override
	public byte[] get(byte[] key) throws NoSuchElementException {
		ByteIterable result = store.get(txn, ByteIterable.fromBytes(key));
		if (result == null)
			throw new NoSuchElementException("no Merkle-Patricia trie node");
		else
			return result.getBytes();
	}
}