package io.hotmoka.node.local;

import java.util.concurrent.ExecutorService;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.internal.store.trie.AbstractTrieBasedStoreTransformationImpl;

public abstract class AbstractTrieBasedStoreTransformation<S extends AbstractTrieBasedStore<S, T>, T extends AbstractTrieBasedStoreTransformation<S, T>> extends AbstractTrieBasedStoreTransformationImpl<S, T> {

	protected AbstractTrieBasedStoreTransformation(S store, ExecutorService executors, ConsensusConfig<?,?> consensus, long now) throws StoreException {
		super(store, executors, consensus, now);
	}
}