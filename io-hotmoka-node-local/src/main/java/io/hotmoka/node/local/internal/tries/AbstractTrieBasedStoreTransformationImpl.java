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

package io.hotmoka.node.local.internal.tries;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.local.AbstractStoreTransformation;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.StateId;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.xodus.env.Transaction;

/**
 * Partial implementation of a store transformation for trie-based stores.
 * 
 * @param <N> the type of the node whose store performs this transformation
 * @param <C> the type of the configuration of the node whose store performs this transformation
 * @param <S> the type of the store that performs this transformation
 * @param <T> the type of this store transformation
 */
public abstract class AbstractTrieBasedStoreTransformationImpl<N extends AbstractTrieBasedLocalNodeImpl<N,C,S,T>, C extends LocalNodeConfig<C,?>, S extends AbstractTrieBasedStoreImpl<N,C,S,T>, T extends AbstractTrieBasedStoreTransformationImpl<N,C,S,T>> extends AbstractStoreTransformation<N,C,S,T> {

	/**
	 * Creates a transformation whose transaction are executed with the given executors.
	 * 
	 * @param store the initial store of the transformation
	 * @param consensus the consensus to use for the execution of transactions in the transformation
	 * @param now the current time to use for the execution of transactions in the transformation
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	protected AbstractTrieBasedStoreTransformationImpl(S store, ConsensusConfig<?,?> consensus, long now) throws StoreException {
		super(store, consensus, now);
	}

	/**
	 * Yields the state identifier of the final store of this transformation, resulting from the execution
	 * of the delivered requests from the initial store.
	 * 
	 * @param txn the Xodus transaction where the store must be created
	 * @return the state identifier of the final store
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	public final StateId getIdOfFinalStore(Transaction txn) throws StoreException {
		return getInitialStore().addDelta(getCache(), getDeltaRequests(), getDeltaResponses(), getDeltaHistories(), getDeltaManifest(), txn);
	}
}