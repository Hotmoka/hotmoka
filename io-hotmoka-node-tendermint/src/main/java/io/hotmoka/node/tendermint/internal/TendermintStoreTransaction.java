package io.hotmoka.node.tendermint.internal;

import io.hotmoka.stores.AbstractTrieBasedStoreTransaction;

public class TendermintStoreTransaction extends AbstractTrieBasedStoreTransaction<TendermintStore> {

	protected TendermintStoreTransaction(TendermintStore store, Object lock) {
		super(store, lock);
	}
}