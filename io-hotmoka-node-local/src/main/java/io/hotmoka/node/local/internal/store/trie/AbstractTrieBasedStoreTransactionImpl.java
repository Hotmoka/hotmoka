package io.hotmoka.node.local.internal.store.trie;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractStoreTransaction;
import io.hotmoka.node.local.StoreCache;
import io.hotmoka.node.local.api.StoreException;

public abstract class AbstractTrieBasedStoreTransactionImpl<S extends AbstractTrieBasedStoreImpl<S, T>, T extends AbstractTrieBasedStoreTransactionImpl<S, T>> extends AbstractStoreTransaction<S, T> {

	protected AbstractTrieBasedStoreTransactionImpl(S store, ExecutorService executors, ConsensusConfig<?,?> consensus, long now) throws StoreException {
		super(store, executors, consensus, now);
	}

	@Override
	protected final S mkFinalStore(StoreCache cache,
			Map<TransactionReference, TransactionRequest<?>> addedRequests,
			Map<TransactionReference, TransactionResponse> addedResponses,
			Map<StorageReference, TransactionReference[]> addedHistories,
			Optional<StorageReference> addedManifest) throws StoreException {

		return getInitialStore().makeNext(cache, addedRequests, addedResponses, addedHistories, addedManifest);
	}
}