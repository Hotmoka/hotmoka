package io.hotmoka.takamaka.internal;

import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.stores.FullTrieBasedStore;
import io.hotmoka.takamaka.TakamakaBlockchainConfig;

/**
 * A full trie-based store for the Takamaka blockchain. By using a full store,
 * that keeps also requests and errors, there is less burden on the Takamaka blockchain,
 * hence the integration is easier than with a partial store.
 */
@ThreadSafe
class Store extends FullTrieBasedStore<TakamakaBlockchainConfig> {

	/**
     * Creates a store for the Takamaka blockchain.
     * It is initialized to the view of the last checked out root.
     * 
     * @param node the node having this store
     */
    Store(TakamakaBlockchainImpl node) {
    	super(node);

    	setRootsAsCheckedOut();
    }

    /**
     * Creates a clone of the given store.
     * 
	 * @param parent the store to clone
     */
    Store(Store parent) {
    	super(parent);
    }

    @Override
    public Store copy() {
    	return new Store(this);
    }
}