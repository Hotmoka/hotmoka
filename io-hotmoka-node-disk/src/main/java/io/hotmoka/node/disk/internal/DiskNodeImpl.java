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

import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.constants.Constants;
import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.disk.api.DiskNode;
import io.hotmoka.node.disk.api.DiskNodeConfig;
import io.hotmoka.node.local.AbstractLocalNode;
import io.hotmoka.node.local.NodeCreationException;
import io.hotmoka.node.local.api.StoreException;

/**
 * An implementation of a node that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining,
 * nor transactions. Updates are stored in files, rather than in an external database.
 */
@ThreadSafe
public class DiskNodeImpl extends AbstractLocalNode<DiskNodeImpl, DiskNodeConfig, DiskStore, DiskStoreTransformation> implements DiskNode {

	/**
	 * The mempool where transaction requests are stored and eventually executed.
	 */
	private final Mempool mempool;

	/**
	 * The store of the head of this node.
	 */
	private volatile DiskStore storeOfHead;

	/**
	 * The path where the blocks of this node are saved on disk.
	 */
	private final Path storePath;

	/**
	 * Builds a new disk memory node.
	 * 
	 * @param config the configuration of the node
	 * @throws NodeCreationException if the node could not be created
	 */
	public DiskNodeImpl(DiskNodeConfig config) throws NodeCreationException {
		super(config, true);

		try {
			this.storePath = getLocalConfig().getDir().resolve("hotmoka").resolve("store");
			this.storeOfHead = mkEmptyStore();
			this.mempool = new Mempool();
		}
		catch (ClosedNodeException e) {
			throw new RuntimeException("The node is unexpectedly closed before its same creation", e);
		}
		catch (NodeException e) { // TODO
			close();
			throw new NodeCreationException(e);
		}
	}

	@Override
	public NodeInfo getInfo() throws ClosedNodeException {
		try (var scope = mkScope()) {
			return NodeInfos.of(DiskNode.class.getName(), Constants.HOTMOKA_VERSION, "");
		}
	}

	@Override
	protected DiskStore mkEmptyStore() throws NodeException {
		try {
			return new DiskStore(this, storePath);
		}
		catch (StoreException e) {
			throw new NodeException(e);
		}
	}

	@Override
	protected DiskStore enterHead() {
		return storeOfHead;
	}

	@Override
	protected void closeResources() {
		try {
			mempool.stop();
		}
		finally {
			super.closeResources();
		}
	}

	@Override
	protected void postRequest(TransactionRequest<?> request) throws InterruptedException, TimeoutException {
		mempool.add(request);
	}

	/**
	 * A mempool receives transaction requests and schedules them for execution,
	 * respecting the order in which they have been proposed.
	 */
	private class Mempool {
		private final static int MAX_CAPACITY = 200_000;

		private final static Logger LOGGER = Logger.getLogger(Mempool.class.getName());

		/**
		 * The queue of requests to deliver.
		 */
		private final BlockingQueue<TransactionRequest<?>> mempool = new LinkedBlockingDeque<>(MAX_CAPACITY);

		/**
		 * The thread that executes requests that have already been checked.
		 */
		private final Thread deliverer;

		/**
		 * The maximal number of transactions to fit in a block.
		 */
		private final int transactionsPerBlock;
	
		/**
		 * Builds a mempool.
		 */
		private Mempool() throws ClosedNodeException {
			this.transactionsPerBlock = getLocalConfig().getTransactionsPerBlock();
			this.deliverer = new Thread(this::deliver);
			this.deliverer.start();
		}

		/**
		 * Adds a request to the mempool. Eventually, it will be checked and executed.
		 * 
		 * @param request the request
		 * @throws InterruptedException 
		 * @throws TimeoutException 
		 */
		private void add(TransactionRequest<?> request) throws InterruptedException, TimeoutException {
			if (!mempool.offer(request, 10, TimeUnit.MILLISECONDS))
				throw new TimeoutException("Mempool overflow");
		}

		/**
		 * Stops the mempool, by stopping its working thread.
		 */
		private void stop() {
			deliverer.interrupt();
		}

		/**
		 * The body of the thread that executes requests. Its pops a request from the checked mempool and executes it.
		 */
		private void deliver() {
			try {
				DiskStoreTransformation transaction = storeOfHead.beginTransformation(System.currentTimeMillis());

				while (true) {
					TransactionRequest<?> current = mempool.poll(2, TimeUnit.MILLISECONDS);
					if (current == null)
						// if no request is available, we create a block, which increases the timestamp of the topmost block:
						// this simulates the passing of the time
						transaction = restartTransaction(transaction);
					else {
						try {
							transaction.deliverTransaction(current);

							if (transaction.deliveredCount() == transactionsPerBlock - 1)
								transaction = restartTransaction(transaction);
						}
						catch (TransactionRejectedException e) {
							signalRejected(current, e);
						}
					}
				}
			}
			catch (StoreException | NodeException e) {
				LOGGER.log(Level.SEVERE, "transaction delivery failure", e);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		/**
		 * Creates a block with all transactions executed since the last block, including the coinbase
		 * transaction, and restarts a new store transformation for the next block.
		 * 
		 * @param transformation the store transformation containing all executed transactions since the last block
		 * @return a new store transformation, where the subsequent transactions can be accumulated
		 * @throws NodeException if the node is misbehaving
		 * @throws InterruptedException if the current thread is interrupted while the coinbase transaction is executing
		 */
		private DiskStoreTransformation restartTransaction(DiskStoreTransformation transformation) throws NodeException, InterruptedException {
			try {
				// if we delivered zero transactions, we prefer to avoid the creation of an empty block
				if (transformation.deliveredCount() > 0) {
					transformation.deliverCoinbaseTransactions();
					storeOfHead = transformation.getFinalStore();
					publishAllTransactionsDeliveredIn(transformation, storeOfHead);
				}

				return storeOfHead.beginTransformation(System.currentTimeMillis());
			}
			catch (StoreException e) {
				throw new NodeException(e);
			}
		}
	}
}