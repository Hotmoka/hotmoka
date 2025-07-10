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

package io.hotmoka.node.local.internal.tries;

import io.hotmoka.patricia.api.KeyValueStore;
import io.hotmoka.patricia.api.UnknownKeyException;
import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;

/**
 * A key/value store implemented over the Xodus database.
 */
public class KeyValueStoreOnXodus implements KeyValueStore {
	private final Store store;
	private final Transaction txn;

	public KeyValueStoreOnXodus(Store store, Transaction txn) {
		this.store = store;
		this.txn = txn;
	}

	@Override
	public byte[] get(byte[] key) throws UnknownKeyException {
		ByteIterable result = store.get(txn, ByteIterable.fromBytes(key));
		if (result == null)
			throw new UnknownKeyException();

		return result.getBytes();
	}

	@Override
	public void put(byte[] key, byte[] value) {
		store.put(txn, ByteIterable.fromBytes(key), ByteIterable.fromBytes(value));
	}

	@Override
	public void remove(byte[] key) throws UnknownKeyException {
		if (!store.delete(txn, ByteIterable.fromBytes(key)))
			throw new UnknownKeyException();
	}
}