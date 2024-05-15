package io.hotmoka.node.disk.internal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.AbstractStoreTransaction;
import io.hotmoka.node.local.LRUCache;
import io.hotmoka.node.local.StoreCache;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.StoreException;

public class DiskStoreTransaction extends AbstractStoreTransaction<DiskStore, DiskStoreTransaction> {

	public DiskStoreTransaction(DiskStore store, ExecutorService executors, ConsensusConfig<?,?> consensus, long now) {
		super(store, executors, consensus, now);
	}

	@Override
	protected DiskStore mkFinalStore(LRUCache<TransactionReference, Boolean> checkedSignatures, LRUCache<TransactionReference, EngineClassLoader> classLoaders,
    		StoreCache cache, Map<TransactionReference, TransactionRequest<?>> addedRequests,
    		Map<TransactionReference, TransactionResponse> addedResponses,
    		Map<StorageReference, TransactionReference[]> addedHistories,
    		Optional<StorageReference> addedManifest) throws StoreException {
	
		return new DiskStore(getInitialStore(), checkedSignatures, classLoaders, cache, addedRequests, addedResponses, addedHistories, addedManifest);
	}
}