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

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.api.UnknownStateIdException;
import io.hotmoka.node.local.internal.tries.AbstractTrieBasedStoreImpl;

/**
 * A store of a node, based on tries. It is a container of request/response pairs.
 * Stores are immutable and consequently thread-safe. Its states are arrays of 128 bytes.
 * It uses an array of 0's to represent the empty store.
 * 
 * @param <N> the type of the node having this store
 * @param <C> the type of the configuration of the node having this store
 * @param <S> the type of this store
 * @param <T> the type of the store transformations that can be started from this store
 */
@Immutable
public abstract class AbstractTrieBasedStore<N extends AbstractTrieBasedLocalNode<N,C,S,T>, C extends LocalNodeConfig<C,?>, S extends AbstractTrieBasedStore<N,C,S,T>, T extends AbstractTrieBasedStoreTransformation<N,C,S,T>> extends AbstractTrieBasedStoreImpl<N,C,S,T> {

	/**
	 * Creates an empty store.
	 * 
	 * @param node the node for which the store is created
	 * @throws StoreException if the operation cannot be completed correctly
	 */
    protected AbstractTrieBasedStore(N node) throws StoreException {
    	super(node);
    }

    /**
	 * Creates a clone of a store, up to the cache.
	 * 
	 * @param toClone the store to clone
	 * @param cache the cache to use in the cloned store
	 */
    protected AbstractTrieBasedStore(AbstractTrieBasedStore<N,C,S,T> toClone, StoreCache cache) {
    	super(toClone, cache);
    }

	/**
	 * Creates a clone of a store, up to cache and roots.
	 * 
	 * @param toClone the store to clone
	 * @param cache to caches to use in the cloned store
	 * @param rootOfResponses the root to use for the tries of responses
	 * @param rootOfInfo the root to use for the tries of infos
	 * @param rootOfHistories the root to use for the tries of histories
	 * @param rootOfRequests the root to use for the tries of requests
     * @param checkExistence true if and only if the existence of the resulting store must be checked
     * @throws UnknownStateIdException if {@code checkExistence} is true and the store does not exist
     * @throws StoreException if the operation could not be completed correctly
	 */
    protected AbstractTrieBasedStore(AbstractTrieBasedStore<N,C,S,T> toClone, StoreCache cache, byte[] rootOfResponses, byte[] rootOfInfo, byte[] rootOfHistories, byte[] rootOfRequests, boolean checkExistence) throws UnknownStateIdException, StoreException {
    	super(toClone, cache, rootOfResponses, rootOfInfo, rootOfHistories, rootOfRequests, checkExistence);
    }

	/**
	 * Creates a clone of a store, up to cache and roots. It does not check for the existence of the result.
	 * 
	 * @param toClone the store to clone
	 * @param cache to caches to use in the cloned store
	 * @param rootOfResponses the root to use for the tries of responses
	 * @param rootOfInfo the root to use for the tries of infos
	 * @param rootOfHistories the root to use for the tries of histories
	 * @param rootOfRequests the root to use for the tries of requests
	 */
    protected AbstractTrieBasedStore(AbstractTrieBasedStore<N,C,S,T> toClone, StoreCache cache, byte[] rootOfResponses, byte[] rootOfInfo, byte[] rootOfHistories, byte[] rootOfRequests) {
    	super(toClone, cache, rootOfResponses, rootOfInfo, rootOfHistories, rootOfRequests);
    }
}