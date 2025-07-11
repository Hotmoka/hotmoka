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

package io.hotmoka.node.mokamint.internal;

import java.util.Optional;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.local.AbstractTrieBasedStore;
import io.hotmoka.node.local.api.StateId;
import io.hotmoka.node.local.api.StoreCache;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.api.UnknownStateIdException;
import io.hotmoka.node.mokamint.api.MokamintNodeConfig;

/**
 * A trie-based store for Mokamint.
 */
@Immutable
public class MokamintStore extends AbstractTrieBasedStore<MokamintNodeImpl, MokamintNodeConfig, MokamintStore, MokamintStoreTransformation> {

	/**
     * Creates an empty store for the Mokamint blockchain, with empty cache.
	 * 
	 * @param node the node for which the store is created
	 * @throws StoreException if the operation cannot be completed correctly
	 */
    MokamintStore(MokamintNodeImpl node) throws StoreException {
    	super(node);
    }

    /**
	 * Creates a clone of a store, up to cache and roots.
	 * 
	 * @param toClone the store to clone
	 * @param cache to caches to use in the cloned store
	 * @param stateId the state identifier of the store to create
     * @throws UnknownStateIdException if the required state does not exist
     * @throws StoreException if the operation could not be completed correctly
	 */
    private MokamintStore(MokamintStore toClone, StateId stateId, Optional<StoreCache> cache) throws UnknownStateIdException, StoreException {
    	super(toClone, stateId, cache);
	}

	/**
	 * Creates a clone of a store, up to cache.
	 * 
	 * @param toClone the store to clone
	 * @param cache the cache to use in the cloned store
	 */
    private MokamintStore(MokamintStore toClone, StoreCache cache) {
    	super(toClone, cache);
	}

    @Override
	public MokamintStore checkedOutAt(StateId stateId, Optional<StoreCache> cache) throws UnknownStateIdException, StoreException, InterruptedException {
		var result = new MokamintStore(this, stateId, cache);
		return cache.isPresent() ? result : result.withReloadedCache();
	}

	@Override
    protected MokamintStore withCache(StoreCache cache) {
    	return new MokamintStore(this, cache);
    }

	@Override
	protected MokamintStoreTransformation beginTransformation(ConsensusConfig<?,?> consensus, long now) throws StoreException {
		return new MokamintStoreTransformation(this, consensus, now);
	}
}