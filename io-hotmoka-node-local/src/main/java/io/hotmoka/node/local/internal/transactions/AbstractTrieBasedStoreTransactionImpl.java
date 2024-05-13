package io.hotmoka.node.local.internal.transactions;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractStoreTransaction;
import io.hotmoka.node.local.LRUCache;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.StoreException;

public abstract class AbstractTrieBasedStoreTransactionImpl<S extends AbstractTrieBasedStoreImpl<S, T>, T extends AbstractTrieBasedStoreTransactionImpl<S, T>> extends AbstractStoreTransaction<S, T> {

	protected AbstractTrieBasedStoreTransactionImpl(S store, ExecutorService executors, ConsensusConfig<?,?> consensus, long now) throws StoreException {
		super(store, executors, consensus, now);
	}

	@Override
	protected final S mkFinalStore(LRUCache<TransactionReference, Boolean> checkedSignatures,
			LRUCache<TransactionReference, EngineClassLoader> classLoaders, ConsensusConfig<?, ?> consensus,
			Optional<BigInteger> gasPrice, OptionalLong inflation,
			Map<TransactionReference, TransactionRequest<?>> addedRequests,
			Map<TransactionReference, TransactionResponse> addedResponses,
			Map<StorageReference, TransactionReference[]> addedHistories,
			Optional<StorageReference> addedManifest) throws StoreException {

		return getInitialStore().makeNext(checkedSignatures, classLoaders, consensus, gasPrice, inflation, addedRequests, addedResponses, addedHistories, addedManifest);
	}
}