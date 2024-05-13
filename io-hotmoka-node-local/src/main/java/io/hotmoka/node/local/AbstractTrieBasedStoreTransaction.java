package io.hotmoka.node.local;

import java.util.concurrent.ExecutorService;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.internal.store.AbstractTrieBasedStoreTransactionImpl;

public abstract class AbstractTrieBasedStoreTransaction<S extends AbstractTrieBasedStore<S, T>, T extends AbstractTrieBasedStoreTransaction<S, T>> extends AbstractTrieBasedStoreTransactionImpl<S, T> {

	protected AbstractTrieBasedStoreTransaction(S store, ExecutorService executors, ConsensusConfig<?,?> consensus, long now) throws StoreException {
		super(store, executors, consensus, now);
	}
}