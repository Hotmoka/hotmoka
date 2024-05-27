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

package io.hotmoka.node.local;

import java.util.concurrent.ExecutorService;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.internal.store.trie.AbstractTrieBasedStoreImpl;
import io.hotmoka.xodus.env.Environment;

/**
 * A store of a node, based on tries. It is a container of request/response pairs.
 * Stores are immutable and consequently thread-safe. Its states are arrays of 128 bytes.
 * It uses an array of 0's to represent the empty store.
 * 
 * @param <S> the type of this store
 * @param <T> the type of the store transformations that can be started from this store
 */
@Immutable
public abstract class AbstractTrieBasedStore<S extends AbstractTrieBasedStore<S, T>, T extends AbstractTrieBasedStoreTransformation<S, T>> extends AbstractTrieBasedStoreImpl<S, T> {

	/**
	 * Creates an empty store.
	 * 
	 * @param env the Xodus environment to use for storing the tries
	 * @param executors the executors to use for running transactions
	 * @param consensus the consensus configuration of the node having the store
	 * @param config the local configuration of the node having the store
	 * @param hasher the hasher for computing the transaction reference from the requests
	 */
    protected AbstractTrieBasedStore(Environment env, ExecutorService executors, ConsensusConfig<?,?> consensus, LocalNodeConfig<?,?> config, Hasher<TransactionRequest<?>> hasher) {
    	super(env, executors, consensus, config, hasher);
    }

	/**
	 * Creates a clone of a store, up to the cache.
	 * 
	 * @param toClone the store to clone
	 * @param cache the cache to use in the cloned store
	 */
    protected AbstractTrieBasedStore(AbstractTrieBasedStore<S, T> toClone, StoreCache cache) {
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
	 */
    protected AbstractTrieBasedStore(AbstractTrieBasedStore<S, T> toClone, StoreCache cache, byte[] rootOfResponses, byte[] rootOfInfo, byte[] rootOfHistories, byte[] rootOfRequests) {
    	super(toClone, cache, rootOfResponses, rootOfInfo, rootOfHistories, rootOfRequests);
    }
}