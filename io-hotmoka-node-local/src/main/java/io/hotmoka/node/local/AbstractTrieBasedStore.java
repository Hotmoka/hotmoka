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

/**
 * A historical store of a node. It is a transactional database that keeps
 * the successful responses of the Hotmoka transactions
 * but not their requests, histories and errors (for this reason it is <i>partial</i>).
 * Its implementation is based on Merkle-Patricia tries,
 * supported by JetBrains' Xodus transactional database.
 * 
 * The information kept in this store consists of:
 * 
 * <ul>
 * <li> a map from each Hotmoka request reference to the response computed for that request
 * <li> miscellaneous control information, such as where the node's manifest
 *      is installed or the current root and number of commits
 * </ul>
 * 
 * This information is added in store by push methods and accessed through get methods.
 * 
 * This class is meant to be subclassed by specifying where errors, requests and histories are kept.
 */
@Immutable
public abstract class AbstractTrieBasedStore<S extends AbstractTrieBasedStore<S, T>, T extends AbstractTrieBasedStoreTransformation<S, T>> extends AbstractTrieBasedStoreImpl<S, T> {

	/**
	 * Creates a store. Its roots are initialized as in the Xodus store, if present.
	 * 
 	 * @param dir the path where the database of the store is kept
	 */
    protected AbstractTrieBasedStore(ExecutorService executors, ConsensusConfig<?,?> consensus, LocalNodeConfig<?,?> config, Hasher<TransactionRequest<?>> hasher) {
    	super(executors, consensus, config, hasher);
    }

    protected AbstractTrieBasedStore(AbstractTrieBasedStore<S, T> toClone, StoreCache cache) {
    	super(toClone, cache);
    }

    protected AbstractTrieBasedStore(AbstractTrieBasedStore<S, T> toClone, StoreCache cache, byte[] rootOfResponses, byte[] rootOfInfo, byte[] rootOfHistories, byte[] rootOfRequests) {
    	super(toClone, cache, rootOfResponses, rootOfInfo, rootOfHistories, rootOfRequests);
    }
}