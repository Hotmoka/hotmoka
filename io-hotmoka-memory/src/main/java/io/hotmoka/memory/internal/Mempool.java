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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.TransactionRequest;

/**
 * A mempool receives transaction requests and schedules them for execution,
 * respecting the order in which they have been proposed.
 */
class Mempool {
	public final static int MAX_CAPACITY = 200_000;
	private final static Logger logger = Logger.getLogger(Mempool.class.getName());

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
	private final MemoryBlockchainInternal node;

	/**
	 * The thread that checks requests when they are submitted.
	 */
	private final Thread checker;

	/**
	 * The thread the execution requests that have already been checked.
	 */
	private final Thread deliverer;

	/**
	 * Builds a mempool.
	 * 
	 * @param node the node for which the mempool works
	 */
	Mempool(MemoryBlockchainInternal node) {
		this.node = node;
		this.checker = new Thread(this::check);
		this.checker.start();
		this.deliverer = new Thread(this::deliver);
		this.deliverer.start();
	}

	/**
	 * Adds a request to the mempool. Eventually, it will be checked and executed.
	 * 
	 * @param request the request
	 */
	public void add(TransactionRequest<?> request) {
		if (!mempool.offer(request))
			throw new InternalFailureException("mempool overflow");
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
		while (!Thread.currentThread().isInterrupted()) {
			try {
				TransactionRequest<?> current = mempool.take();

				try {
					node.checkTransaction(current);
					if (!checkedMempool.offer(current)) {
						deliverer.interrupt();
						throw new IllegalStateException("mempool overflow");
					}
				}
				catch (TransactionRejectedException e) {
					// already logged
				}
	            catch (Throwable t) {
	            	logger.log(Level.WARNING, "Failed to check transaction request", t);
	    		}
			}
			catch (InterruptedException e) {
				return;
			}
		}
	}

	/**
	 * The body of the thread that executes requests. Its pops a request from the checked mempool and executes it.
	 */
	private void deliver() {
		int counter = 0;
		int transactionsPerBlock = node.getConfig().transactionsPerBlock;

		while (!Thread.currentThread().isInterrupted()) {
			try {
				TransactionRequest<?> current = checkedMempool.take();

				try {
					node.deliverTransaction(current);
					counter = (counter + 1) % transactionsPerBlock;
					// the last transaction of a block is for rewarding the validators and updating the gas price
					if (counter == transactionsPerBlock - 1 && node.rewardValidators("", ""))
						counter = 0;
				}
	            catch (Throwable t) {
	            	logger.log(Level.WARNING, "Failed to deliver transaction request", t);
	    		}
			}
			catch (InterruptedException e) {
				return;
			}
		}
	}
}