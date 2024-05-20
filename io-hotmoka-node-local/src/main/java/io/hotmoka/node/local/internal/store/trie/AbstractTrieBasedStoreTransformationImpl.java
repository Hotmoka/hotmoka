package io.hotmoka.node.local.internal.store.trie;

import java.util.concurrent.ExecutorService;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.local.AbstractStoreTranformation;
import io.hotmoka.node.local.api.StoreException;

public abstract class AbstractTrieBasedStoreTransformationImpl<S extends AbstractTrieBasedStoreImpl<S, T>, T extends AbstractTrieBasedStoreTransformationImpl<S, T>> extends AbstractStoreTranformation<S, T> {

	protected AbstractTrieBasedStoreTransformationImpl(S store, ExecutorService executors, ConsensusConfig<?,?> consensus, long now) throws StoreException {
		super(store, executors, consensus, now);
	}
}