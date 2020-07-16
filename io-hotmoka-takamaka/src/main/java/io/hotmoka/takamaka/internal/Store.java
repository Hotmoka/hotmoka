package io.hotmoka.takamaka.internal;

import io.hotmoka.stores.FullTrieBasedStore;

/**
 * A full trie-based store for the Takamaka blockchain. By using a full store,
 * that keeps also requests and errors, there is less burden on the Takamaka blockchain,
 * hence the integration is easier than with a partial store.
 */
class Store extends FullTrieBasedStore<TakamakaBlockchainImpl> {

	/**
     * Creates a store for the Takamaka blockchain.
     * It is initialized to the view of the last checked out root.
     * 
     * @param node the node for which the store is being built
     */
    Store(TakamakaBlockchainImpl node) {
    	super(node);
    }

    /**
     * Creates a store initialized to the view of the given root.
     * 
	 * @param node the node for which the store is being built
     * @param hash the root to use for the store
     */
    Store(TakamakaBlockchainImpl node, byte[] hash) {
    	super(node, hash);
    }
}