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

package io.hotmoka.memory.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.nodes.NodeInfo;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithEvents;
import io.hotmoka.constants.Constants;
import io.hotmoka.local.AbstractLocalNode;
import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.memory.MemoryBlockchainConfig;
import io.hotmoka.nodes.ConsensusParams;

/**
 * An implementation of a blockchain that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining,
 * nor transactions. Updates are stored in files, rather than in an external database.
 */
@ThreadSafe
public class MemoryBlockchainImpl extends AbstractLocalNode<MemoryBlockchainConfig, Store> implements MemoryBlockchain {
	private final static Logger logger = Logger.getLogger(MemoryBlockchainImpl.class.getName());

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
	public MemoryBlockchainImpl(MemoryBlockchainConfig config, ConsensusParams consensus) {
		super(config, consensus);

		try {
			this.mempool = new Mempool(new MemoryBlockchainInternalImpl());
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "failed creating memory blockchain", e);

			try {
				close();
			}
			catch (Exception e1) {
				logger.log(Level.SEVERE, "cannot close the blockchain", e1);
				throw InternalFailureException.of(e1);
			}

			throw InternalFailureException.of(e);
		}
	}

	@Override
	protected Store mkStore() {
		return new Store(this);
	}

	@Override
	public void close() throws Exception {
		if (isNotYetClosed()) {
			mempool.stop();
			super.close();
		}
	}

	@Override
	public NodeInfo getNodeInfo() {
		return new NodeInfo(MemoryBlockchain.class.getName(), Constants.VERSION, "");
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

	private class MemoryBlockchainInternalImpl implements MemoryBlockchainInternal {

		@Override
		public MemoryBlockchainConfig getConfig() {
			return config;
		}

		@Override
		public void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
			MemoryBlockchainImpl.this.checkTransaction(request);
		}

		@Override
		public TransactionResponse deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException {
			return MemoryBlockchainImpl.this.deliverTransaction(request);
		}

		@Override
		public boolean rewardValidators(String behaving, String misbehaving) {
			return MemoryBlockchainImpl.this.rewardValidators(behaving, misbehaving);
		}
	}
}