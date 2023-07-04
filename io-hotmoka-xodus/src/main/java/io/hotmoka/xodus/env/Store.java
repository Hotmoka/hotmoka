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

import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.xodus.ByteIterable;
import io.hotmoka.xodus.ExodusException;

public class Store {
	protected final static Logger LOGGER = Logger.getLogger(Store.class.getName());
	private final jetbrains.exodus.env.Store parent;

	Store(jetbrains.exodus.env.Store parent) {
		this.parent = parent;
	}

	public void put(Transaction txn, ByteIterable key, ByteIterable value) throws ExodusException {
		try {
			if (!parent.put(txn.toNative(), key.toNative(), value.toNative()))
				LOGGER.log(Level.SEVERE, "couldn't write key " + key + " into the Xodus store");
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

	public ByteIterable get(Transaction txn, ByteIterable key) throws ExodusException {
		try {
			return ByteIterable.fromNative(parent.get(txn.toNative(), key.toNative()));
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}

	public void remove(Transaction txn, ByteIterable key) throws ExodusException {
		try {
			if (!parent.delete(txn.toNative(), key.toNative()))
				LOGGER.log(Level.SEVERE, "couldn't delete key " + key + " from the Xodus store");
		}
		catch (jetbrains.exodus.ExodusException e) {
			throw new ExodusException(e);
		}
	}
}