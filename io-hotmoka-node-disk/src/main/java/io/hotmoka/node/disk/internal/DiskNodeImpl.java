/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.disk.internal;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.ClosedNodeException;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.disk.api.DiskNode;
import io.hotmoka.node.disk.api.DiskNodeConfig;
import io.hotmoka.node.local.AbstractLocalNode;

/**
 * An implementation of a node that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining,
 * nor transactions. Updates are stored in files, rather than in an external database.
 */
@ThreadSafe
public class DiskNodeImpl extends AbstractLocalNode<DiskNodeImpl, DiskNodeConfig, DiskStore> implements DiskNode {

	/**
	 * The mempool where transaction requests are stored and eventually executed.
	 */
	private final Mempool mempool;

	/**
	 * Builds a brand new blockchain in disk memory.
	 * 
	 * @param config the configuration of the blockchain
	 * @param consensus the consensus parameters of the blockchain
	 * @throws NodeException 
	 */
	public DiskNodeImpl(DiskNodeConfig config, ConsensusConfig<?,?> consensus) throws NodeException {
		super(config, consensus);

		this.mempool = new Mempool(this, (int) config.getTransactionsPerBlock()); // TODO: make this option int
	}

	@Override
	public DiskNodeConfig getLocalConfig() {
		return config;
	}

	@Override
	protected DiskStore mkStore(ConsensusConfig<?,?> consensus) {
		return new DiskStore(this, consensus);
	}

	@Override
	protected void closeResources() throws NodeException, InterruptedException {
		try {
			mempool.stop();
		}
		finally {
			super.closeResources();
		}
	}

	@Override
	public NodeInfo getNodeInfo() throws ClosedNodeException {
		try (var scope = mkScope()) {
			return NodeInfos.of(DiskNode.class.getName(), HOTMOKA_VERSION, "");
		}
	}

	@Override
	protected void postRequest(TransactionRequest<?> request) {
		mempool.add(request);
	}
}