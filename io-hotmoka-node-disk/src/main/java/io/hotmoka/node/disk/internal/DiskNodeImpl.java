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

import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.ClosedNodeException;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithEvents;
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
public class DiskNodeImpl extends AbstractLocalNode<DiskNodeConfig, DiskStore> implements DiskNode {
	private final static Logger LOGGER = Logger.getLogger(DiskNodeImpl.class.getName());

	/**
	 * The mempool where transaction requests are stored and eventually executed.
	 */
	private final Mempool mempool;

	/**
	 * Builds a brand new blockchain in disk memory.
	 * 
	 * @param config the configuration of the blockchain
	 * @param consensus the consensus parameters of the blockchain
	 */
	public DiskNodeImpl(DiskNodeConfig config, ConsensusConfig<?,?> consensus) {
		super(config, consensus);

		try {
			this.mempool = new Mempool(new MemoryBlockchainInternalImpl());
		}
		catch (RuntimeException e) {
			LOGGER.log(Level.SEVERE, "failed to create the memory blockchain", e);

			try {
				close();
			}
			catch (Exception e1) {
				LOGGER.log(Level.SEVERE, "cannot close the blockchain", e1);
			}

			throw e;
		}
	}

	@Override
	public DiskNodeConfig getLocalConfig() {
		return config;
	}

	@Override
	protected DiskStore mkStore() {
		return new DiskStore(getLocalConfig().getDir(), getLocalConfig().getTransactionsPerBlock());
	}

	@Override
	protected long getNow() {
		return System.currentTimeMillis();
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

	@Override
	protected void scheduleForNotificationOfEvents(TransactionResponseWithEvents response) {
		// immediate notification, since there is no commit
		notifyEventsOf(response);
	}

	private class MemoryBlockchainInternalImpl implements DiskNodeInternal {

		@Override
		public DiskNodeConfig getConfig() {
			return config;
		}

		@Override
		public void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
			DiskNodeImpl.this.checkTransaction(request);
		}

		@Override
		public TransactionResponse deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
			return DiskNodeImpl.this.deliverTransaction(request);
		}

		@Override
		public boolean rewardValidators(String behaving, String misbehaving) {
			return DiskNodeImpl.this.rewardValidators(behaving, misbehaving);
		}
	}
}