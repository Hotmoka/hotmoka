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

import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.local.api.StoreException;

/**
 * A mempool receives transaction requests and schedules them for execution,
 * respecting the order in which they have been proposed.
 */
class Mempool {
	public final static int MAX_CAPACITY = 200_000;

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
	 * The node for which requests are executed.
	 */
	private final DiskNodeImpl node;

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
	 * 
	 * @param node the node for which the mempool works
	 */
	Mempool(DiskNodeImpl node, int transactionsPerBlock) throws NodeException {
		this.node = node;
		this.transactionsPerBlock = transactionsPerBlock;
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
	public void add(TransactionRequest<?> request) throws InterruptedException, TimeoutException {
		if (!mempool.offer(request, 10, TimeUnit.MILLISECONDS))
			throw new TimeoutException("Mempool overflow");
	}

	/**
	 * Stops the mempool, by stopping its working threads.
	 */
	public void stop() {
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

				try {
					node.checkTransaction(current);
					if (!checkedMempool.offer(current)) {
						deliverer.interrupt();
						throw new NodeException("Mempool overflow");
					}
				}
				catch (TransactionRejectedException e) {
					node.signalRejected(current, e);
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
			DiskStoreTransformation transaction = node.beginTransaction(System.currentTimeMillis());

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
						node.signalRejected(current, e);
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

	private DiskStoreTransformation restartTransaction(DiskStoreTransformation transaction) throws NodeException {
		// if we delivered zero transactions, we prefer to avoid the creation of an empty block
		if (transaction.deliveredCount() > 0) {
			try {
				transaction.deliverRewardTransaction("", "");
			}
			catch (StoreException e) {
				throw new NodeException(e);
			}

			node.moveToFinalStoreOf(transaction);
		}

		return node.beginTransaction(System.currentTimeMillis());
	}
}