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

package io.hotmoka.stores.internal;

import java.util.NoSuchElementException;

import io.hotmoka.patricia.api.KeyValueStore;
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