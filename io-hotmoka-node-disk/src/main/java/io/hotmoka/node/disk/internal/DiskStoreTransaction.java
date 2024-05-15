package io.hotmoka.node.disk.internal;

import java.util.concurrent.ExecutorService;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.local.AbstractStoreTransaction;

public class DiskStoreTransaction extends AbstractStoreTransaction<DiskStore, DiskStoreTransaction> {

	public DiskStoreTransaction(DiskStore store, ExecutorService executors, ConsensusConfig<?,?> consensus, long now) {
		super(store, executors, consensus, now);
	}
}