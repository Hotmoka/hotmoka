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

package io.hotmoka.stores;

import io.hotmoka.annotations.ThreadSafe;

/**
 * Shared implementation of the store of a node. It keeps information about the state of the objects created
 * by the requests executed by the node.
 */
@ThreadSafe
public abstract class AbstractStore<T extends AbstractStore<T>> implements Store<T> {

	/**
	 * The lock for modifications of the store.
	 */
	protected final Object lock;

	/**
	 * Builds the store for a node.
	 */
	protected AbstractStore() {
		this.lock = new Object();
	}

	/**
	 * Creates a clone of the given store.
	 * 
	 * @param toClone the store to clone
	 */
	protected AbstractStore(AbstractStore<T> toClone) {
		this.lock = toClone.lock;
	}

	protected abstract T mkClone();

	protected abstract T getThis();

	@Override
	public void close() {}
}