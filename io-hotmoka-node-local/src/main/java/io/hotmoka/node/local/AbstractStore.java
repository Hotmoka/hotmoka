/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.local;

import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.Store;

public abstract class AbstractStore<T extends AbstractStore<T>> implements Store<T> {

	/**
	 * Cached recent requests that have had their signature checked.
	 * This can be shared across distinct stores since valid signatures
	 * remain valid over time.
	 */
	final LRUCache<SignedTransactionRequest<?>, Boolean> checkedSignatures;

	/**
	 * The cache for the class loaders. This can be shared across distinct stores since
	 * jars installed in store remain valid over time.
	 */
	final LRUCache<TransactionReference, EngineClassLoader> classLoaders;

	protected AbstractStore() {
		this.checkedSignatures = new LRUCache<>(100, 1000);
		this.classLoaders = new LRUCache<>(100, 1000);
	}

	protected AbstractStore(AbstractStore<T> toClone) {
		this.checkedSignatures = toClone.checkedSignatures;
		this.classLoaders = toClone.classLoaders;
	}
}