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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.ClosedNodeException;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.disk.api.DiskNode;
import io.hotmoka.node.disk.api.DiskNodeConfig;
import io.hotmoka.node.local.AbstractLocalNode;
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
	 * Builds a new disk memory node.
	 * 
	 * @param config the configuration of the node
	 * @throws NodeException if the operation cannot be completed correctly
	 * @throws InterruptedException if the current thread is interrupted before completing the operation
	 */
	public DiskNodeImpl(DiskNodeConfig config) throws NodeException, InterruptedException {
		super(config, true);

		try {
			this.storeOfHead = mkEmptyStore();
			this.mempool = new Mempool();
		}
		catch (NodeException e) {
			close();
			throw e;
		}
	}

	@Override
	public NodeInfo getInfo() throws ClosedNodeException {
		try (var scope = mkScope()) {
			return NodeInfos.of(DiskNode.class.getName(), HOTMOKA_VERSION, "");
		}
	}

	@Override
	protected DiskStore mkEmptyStore() throws NodeException {
		try {
			return new DiskStore(this);
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
	protected void closeResources() throws NodeException {
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
		 * The queue of requests to check.
		 */
		private final BlockingQueue<TransactionRequest<?>> mempool = new LinkedBlockingDeque<>(MAX_CAPACITY);

		/**
		 * The queue of the already checked requests, that still need to be executed.
		 */
		private final BlockingQueue<TransactionRequest<?>> checkedMempool = new LinkedBlockingDeque<>(MAX_CAPACITY);

		/**
		 * The thread that checks requests when they are submitted.
		 */
		private final Thread checker;

		/**
		 * The thread the execution requests that have already been checked.
		 */
		private final Thread deliverer;

		private final int transactionsPerBlock;
	
		/**
		 * Builds a mempool.
		 */
		private Mempool() throws NodeException {
			this.transactionsPerBlock = getLocalConfig().getTransactionsPerBlock();
			this.checker = new Thread(this::check);
			this.checker.start();
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
		 * Stops the mempool, by stopping its working threads.
		 */
		private void stop() {
			checker.interrupt();
			deliverer.interrupt();
		}

		/**
		 * The body of the checking thread. Its pops a request from the mempool and checks it.
		 */
		private void check() {
			try {
				while (true) {
					TransactionRequest<?> current = mempool.take();

					if (!checkedMempool.offer(current)) {
						deliverer.interrupt();
						throw new NodeException("Mempool overflow");
					}
				}
			}
			catch (NodeException e) {
				LOGGER.log(Level.SEVERE, "transaction check failure", e);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			catch (RuntimeException e) {
				LOGGER.log(Level.SEVERE, "Unexpected exception", e);
			}
		}

		/**
		 * The body of the thread that executes requests. Its pops a request from the checked mempool and executes it.
		 */
		private void deliver() {
			try {
				DiskStoreTransformation transaction = storeOfHead.beginTransformation(System.currentTimeMillis());

				while (true) {
					TransactionRequest<?> current = checkedMempool.poll(2, TimeUnit.MILLISECONDS);
					if (current == null)
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
			catch (RuntimeException e) {
				LOGGER.log(Level.SEVERE, "Unexpected exception", e);
			}
		}

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