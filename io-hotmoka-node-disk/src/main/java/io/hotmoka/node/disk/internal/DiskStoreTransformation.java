package io.hotmoka.node.disk.internal;

import java.util.concurrent.ExecutorService;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.local.AbstractStoreTranformation;

public class DiskStoreTransformation extends AbstractStoreTranformation<DiskStore, DiskStoreTransformation> {

	public DiskStoreTransformation(DiskStore store, ExecutorService executors, ConsensusConfig<?,?> consensus, long now) {
		super(store, executors, consensus, now);
	}
}