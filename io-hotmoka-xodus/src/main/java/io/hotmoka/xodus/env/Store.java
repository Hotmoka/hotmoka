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