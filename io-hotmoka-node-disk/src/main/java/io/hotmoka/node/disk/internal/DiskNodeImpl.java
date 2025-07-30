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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.constants.Constants;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.TransactionResponseWithUpdates;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
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
	 * A map that simulates the index of the node: for each object, it associates
	 * the list of transactions that have modified the object.
	 */
	private final ConcurrentMap<StorageReference, List<TransactionReference>> index = new ConcurrentHashMap<>();

	/**
	 * Builds a new disk memory node.
	 * 
	 * @param config the configuration of the node
	 */
	public DiskNodeImpl(DiskNodeConfig config) {
		super(config, true);

		this.storePath = getLocalConfig().getDir().resolve("hotmoka").resolve("store");
		this.storeOfHead = mkEmptyStore();
		this.mempool = new Mempool();
	}

	@Override
	public NodeInfo getInfo() throws ClosedNodeException {
		try (var scope = mkScope()) {
			return NodeInfos.of(DiskNode.class.getName(), Constants.HOTMOKA_VERSION, "");
		}
	}

	@Override
	public Stream<TransactionReference> getIndex(StorageReference reference) throws UnknownReferenceException, ClosedNodeException, InterruptedException {
		try (var scope = mkScope()) {
			var idx = index.get(reference);
			if (idx == null)
				throw new UnknownReferenceException(reference);
			else
				return idx.stream();
		}
	}

	
	@Override
	protected DiskStore mkEmptyStore() {
		return new DiskStore(this, storePath);
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
	 * Expands the index of the node with the objects modified in the given transaction.
	 * 
	 * @param transaction the reference to the transaction
	 * @param response the response of the {@code transaction}
	 */
	private void expandIndex(TransactionReference transaction, TransactionResponse response) {
		if (response instanceof TransactionResponseWithUpdates trwu) // TODO: check if index size is 0
			trwu.getUpdates().map(Update::getObject).distinct().forEach(object -> expandIndex(object, transaction));
	}

	/**
	 * Expands the index of the node, at the given object, with the given transaction.
	 * 
	 * @param object the object whose index gets expanded
	 * @param transaction the transaction added to the index of {@code index}
	 */
	private void expandIndex(StorageReference object, TransactionReference transaction) {
		index.compute(object, (k, v) -> {
			if (v == null)
				v = new LinkedList<>();

			v.add(transaction);
			if (v.size() > 10) // TODO: define constant
				v.remove(0);

			return v;
		});
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
		private Mempool() {
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
					TransactionRequest<?> request = mempool.poll(2, TimeUnit.MILLISECONDS);
					if (request == null)
						// if no request is available, we create a block, which increases the timestamp of the topmost block:
						// this simulates the passing of the time
						transaction = restartTransaction(transaction);
					else {
						try {
							transaction.deliverTransaction(request);

							if (transaction.deliveredCount() == transactionsPerBlock - 1)
								transaction = restartTransaction(transaction);
						}
						catch (TransactionRejectedException e) {
							signalRejected(request, e);
						}
					}
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			catch (RuntimeException e) {
				LOGGER.log(Level.SEVERE, "transaction delivery failure", e);
			}
		}

		/**
		 * Creates a block with all transactions executed since the last block, including the coinbase
		 * transaction, and restarts a new store transformation for the next block.
		 * 
		 * @param transformation the store transformation containing all executed transactions since the last block
		 * @return a new store transformation, where the subsequent transactions can be accumulated
		 * @throws InterruptedException if the current thread is interrupted while the coinbase transaction is executing
		 */
		private DiskStoreTransformation restartTransaction(DiskStoreTransformation transformation) throws InterruptedException {
			// if we delivered zero transactions, we prefer to avoid the creation of an empty block
			if (transformation.deliveredCount() > 0) {
				transformation.deliverCoinbaseTransactions();
				storeOfHead = transformation.getFinalStore();
				publishAllTransactionsDeliveredIn(transformation, storeOfHead);
				transformation.forEachDeliveredTransaction(DiskNodeImpl.this::expandIndex);
			}

			return storeOfHead.beginTransformation(System.currentTimeMillis());
		}
	}
}