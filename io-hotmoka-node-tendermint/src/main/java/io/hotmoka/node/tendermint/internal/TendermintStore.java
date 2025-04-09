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

import java.util.Optional;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.local.AbstractTrieBasedStore;
import io.hotmoka.node.local.api.StateId;
import io.hotmoka.node.local.api.StoreCache;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.api.UnknownStateIdException;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;

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
     * Creates an empty store for the Tendermint blockchain, with empty cache.
	 * 
	 * @param node the node for which the store is created
	 * @throws StoreException if the operation cannot be completed correctly
	 */
    TendermintStore(TendermintNodeImpl node) throws StoreException {
    	super(node);

    	this.validators = Optional.empty();
    }

    /**
	 * Creates a clone of a store, up to cache and state identifier.
	 * 
	 * @param toClone the store to clone
	 * @param cache to caches to use in the cloned store
	 * @param stateId the state identifier of the store to create
     * @throws UnknownStateIdException if the required state does not exist
     * @throws StoreException if the operation could not be completed correctly
	 */
    private TendermintStore(TendermintStore toClone, StateId stateId, Optional<StoreCache> cache) throws UnknownStateIdException, StoreException {
    	super(toClone, stateId, cache);

    	this.validators = toClone.validators;
	}

	/**
	 * Creates a clone of a store, up to its cache.
	 * 
	 * @param toClone the store to clone
	 * @param cache the cache to use in the cloned store
	 * @throws StoreException if the operation cannot be completed correctly
	 */
    private TendermintStore(TendermintStore toClone, StoreCache cache) throws StoreException {
    	super(toClone, cache);

    	this.validators = toClone.validators;
	}

    @Override
    public TendermintStore checkedOutAt(StateId stateId, Optional<StoreCache> cache) throws UnknownStateIdException, StoreException, InterruptedException {
		var result = new TendermintStore(this, stateId, cache);
		return cache.isPresent() ? result : result.withReloadedCache();
	}

	@Override
    protected TendermintStore withCache(StoreCache cache) throws StoreException {
    	return new TendermintStore(this, cache);
    }

	@Override
	protected TendermintStoreTransformation beginTransformation(ConsensusConfig<?,?> consensus, long now) throws StoreException {
		return new TendermintStoreTransformation(this, consensus, now, validators);
	}
}