package io.hotmoka.node.local.internal.store.trie;

import java.util.concurrent.ExecutorService;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.local.AbstractStoreTransaction;
import io.hotmoka.node.local.api.StoreException;

public abstract class AbstractTrieBasedStoreTransactionImpl<S extends AbstractTrieBasedStoreImpl<S, T>, T extends AbstractTrieBasedStoreTransactionImpl<S, T>> extends AbstractStoreTransaction<S, T> {

	protected AbstractTrieBasedStoreTransactionImpl(S store, ExecutorService executors, ConsensusConfig<?,?> consensus, long now) throws StoreException {
		super(store, executors, consensus, now);
	}
}