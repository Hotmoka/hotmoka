package io.hotmoka.tendermint.internal;

import java.util.NoSuchElementException;

import io.hotmoka.patricia.KeyValueStore;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;

public class KeyValueStoreOnXodus implements KeyValueStore {

	private final Store store;
	private final Transaction txn;

	public KeyValueStoreOnXodus(Store store, Transaction txn) {
		this.store = store;
		this.txn = txn;
	}

	/**
     * The key used inside the store to keep the root.
     */
    private final static ByteIterable ROOT = ByteIterable.fromByte((byte) 0);

    @Override
	public byte[] getRoot() {
		ByteIterable root = store.get(txn, ROOT);
		return root == null ? null : root.getBytes();
	}

	@Override
	public void setRoot(byte[] root) {
		store.put(txn, ROOT, ByteIterable.fromBytes(root));
	}

	@Override
	public void put(byte[] key, byte[] value) {
		store.put(txn, ByteIterable.fromBytes(key), ByteIterable.fromBytes(value));
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