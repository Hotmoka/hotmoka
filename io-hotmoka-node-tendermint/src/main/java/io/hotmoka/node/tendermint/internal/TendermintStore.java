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

package io.hotmoka.node.tendermint.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractTrieBasedStore;
import io.hotmoka.node.local.StoreCache;
import io.hotmoka.node.local.api.StateId;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;
import io.hotmoka.xodus.env.Transaction;

/**
 * A partial trie-based store. Errors and requests are recovered by asking
 * Tendermint, since it keeps such information inside its blocks.
 */
@Immutable
public class TendermintStore extends AbstractTrieBasedStore<TendermintNodeImpl, TendermintNodeConfig, TendermintStore, TendermintStoreTransformation> {

	/**
	 * The current validators set in this store transaction. This information could be recovered from the store transaction itself,
	 * but this field is used for caching. The validators set might be missing if the node is not initialized yet.
	 */
	private volatile Optional<TendermintValidator[]> validators;

	/**
     * Creates an empty store for the Tendermint blockchain.
	 * 
	 * @param node the node for which the store is created
	 */
    TendermintStore(TendermintNodeImpl node) throws StoreException {
    	super(node);

    	this.validators = Optional.empty();
    }

    /**
	 * Creates a store checked out at the given state identifier.
	 * 
	 * @param node the node for which the store is created
	 * @param stateId the state identifier
	 * @throws StoreException if the operation cannot be completed correctly
	 */
    TendermintStore(TendermintNodeImpl node, StateId stateId) throws StoreException {
    	super(node, stateId);

    	this.validators = Optional.empty();
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
    private TendermintStore(TendermintStore toClone, StoreCache cache, byte[] rootOfResponses, byte[] rootOfInfo, byte[] rootOfHistories, byte[] rootOfRequests) {
    	super(toClone, cache, rootOfResponses, rootOfInfo, rootOfHistories, rootOfRequests);

    	this.validators = toClone.validators;
	}

	/**
	 * Creates a clone of a store, up to cache.
	 * 
	 * @param toClone the store to clone
	 * @param cache the cache to use in the cloned store
	 */
    private TendermintStore(TendermintStore toClone, StoreCache cache) {
    	super(toClone, cache);

    	this.validators = toClone.validators;
	}

    @Override
    protected TendermintStore addDelta(StoreCache cache, LinkedHashMap<TransactionReference, TransactionRequest<?>> addedRequests,
			Map<TransactionReference, TransactionResponse> addedResponses,
			Map<StorageReference, TransactionReference[]> addedHistories, Optional<StorageReference> addedManifest, Transaction txn) throws StoreException {

    	TendermintStore result = super.addDelta(cache, addedRequests, addedResponses, addedHistories, addedManifest, txn);
    	result.validators = validators;
    	return result;
    }

    @Override
    protected TendermintStore setCache(StoreCache cache) {
    	return new TendermintStore(this, cache);
    }

	@Override
    protected TendermintStore mkStore(StoreCache cache, byte[] rootOfResponses, byte[] rootOfInfo, byte[] rootOfHistories, byte[] rootOfRequests) {
		return new TendermintStore(this, cache, rootOfResponses, rootOfInfo, rootOfHistories, rootOfRequests);
	}

	@Override
	protected TendermintStoreTransformation beginTransformation(ConsensusConfig<?,?> consensus, long now) throws StoreException {
		return new TendermintStoreTransformation(this, consensus, now, validators);
	}
}