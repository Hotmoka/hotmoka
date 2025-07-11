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

import java.util.Optional;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.StoreCache;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.internal.AbstractStoreImpl;

/**
 * A partial store of a node. It is a container of request/response pairs.
 * Stores are immutable and consequently thread-safe.
 * 
 * @param <N> the type of the node having this store
 * @param <C> the type of the configuration of the node having this store
 * @param <S> the type of this store
 * @param <T> the type of the store transformations that can be started from this store
 */
@Immutable
public abstract class AbstractStore<N extends AbstractLocalNode<N,C,S,T>, C extends LocalNodeConfig<C,?>, S extends AbstractStore<N,C,S,T>, T extends AbstractStoreTransformation<N,C,S,T>> extends AbstractStoreImpl<N,C,S,T> {

	/**
	 * Creates an empty store, with empty cache.
	 * 
	 * @param node the node for which the store is created
	 * @throws StoreException if the store could not be created
	 */
	protected AbstractStore(N node) throws StoreException {
		super(node);
	}

	/**
	 * Creates a clone of a store, up to the cache.
	 * 
	 * @param toClone the store to clone
	 * @param cache the cache to use in the cloned store
	 */
	protected AbstractStore(AbstractStore<N,C,S,T> toClone, StoreCache cache) {
		super(toClone, cache);
	}

	/**
	 * Creates a clone of a store, up to the cache.
	 * 
	 * @param toClone the store to clone
	 * @param cache the cache to use in the cloned store
	 * @throws StoreException if the store could not be created
	 */
	protected AbstractStore(AbstractStore<N,C,S,T> toClone, Optional<StoreCache> cache) throws StoreException {
		super(toClone, cache);
	}
}