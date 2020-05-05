package io.hotmoka.memory.internal;

import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.GuardedBy;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.takamaka.code.engine.ResponseBuilder;

/**
 * A mempool receives transaction requests and schedules them for execution,
 * respecting the order in which they have been proposed.
 */
class Mempool {
	public final static int MAX_CAPACITY = 200_000;
	private final static Logger logger = LoggerFactory.getLogger(Mempool.class);

	/**
	 * The queue of requests to check.
	 */
	private final BlockingQueue<RequestWithId> mempool = new LinkedBlockingDeque<>(MAX_CAPACITY);

	/**
	 * The queue of the already checked requests, that still need to be executed.
	 */
	private final BlockingQueue<RequestWithId> checkedMempool = new LinkedBlockingDeque<>(MAX_CAPACITY);

	/**
	 * The node for which requests are executed.
	 */
	private final MemoryBlockchainImpl node;

	private final Object idLock = new Object();

	/**
	 * The next id that can be used for the next submitted request.
	 */
	@GuardedBy("idLock")
	private BigInteger id;

	/**
	 * The thread that checks requests when they are submitted.
	 */
	private final Thread checker;

	/**
	 * The thread the execution requests that have already been checked.
	 */
	private final Thread deliverer;

	/**
	 * The task to run when a request with a given id has been executed and
	 * generated a transaction.
	 */
	private final BiConsumer<String, TransactionReference> transactionReferenceSetter;

	/**
	 * The task to run when a request with a given id has been executed and
	 * generated an error.
	 */
	private final BiConsumer<String, String> transactionErrorSetter;

	/**
	 * Builds a mempool.
	 * 
	 * @param node the node for which the mempool works
	 * @param transactionReferenceSetter a task to run when a request with a given id has been executed and
	 *                                   generated a transaction
	 * @param transactionErrorSetter a task to run when a request with a given id has been executed and
	 *                               generated a transaction
	 */
	Mempool(MemoryBlockchainImpl node, BiConsumer<String, TransactionReference> transactionReferenceSetter, BiConsumer<String, String> transactionErrorSetter) {
		this.node = node;
		this.transactionReferenceSetter = transactionReferenceSetter;
		this.transactionErrorSetter = transactionErrorSetter;
		this.id = BigInteger.ZERO;
		this.checker = new Thread(this::check);
		this.checker.start();
		this.deliverer = new Thread(this::deliver);
		this.deliverer.start();
	}

	/**
	 * Adds a request to the mempool. Eventually, it will be checked and executed.
	 * 
	 * @param request the request
	 * @return the unique identifier that can be used to wait for the outcome of the request
	 */
	public String add(TransactionRequest<?> request) {
		String result;

		synchronized (idLock) {
			result = id.toString();

			if (!mempool.offer(new RequestWithId(request, result)))
				throw new IllegalStateException("mempool overflow");

			id = id.add(BigInteger.ONE);
		}

		return result;
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
				RequestWithId current = mempool.take();

				try {
					node.checkTransaction(current.request);
					if (!checkedMempool.offer(current)) {
						deliverer.interrupt();
						throw new IllegalStateException("mempool overflow");
					}
				}
				catch (TransactionRejectedException e) {
					logger.info("Failed to check transaction request", e);
					transactionErrorSetter.accept(current.id, e.getMessage());
					node.releaseWhoWasWaitingFor(current.request);
				}
	            catch (Throwable t) {
	            	logger.error("Failed to check transaction request", t);
	            	transactionErrorSetter.accept(current.id, t.toString());
	            	node.releaseWhoWasWaitingFor(current.request);
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
		while (!Thread.currentThread().isInterrupted()) {
			try {
				RequestWithId current = checkedMempool.take();

				try {
					ResponseBuilder<?,?> builder = node.checkTransaction(current.request);
					TransactionReference next = node.nextAndIncrement();
					node.deliverTransaction(builder, next);
					transactionReferenceSetter.accept(current.id, next);
				}
				catch (TransactionRejectedException e) {
					logger.info("Failed delivering transaction", e);
					transactionErrorSetter.accept(current.id, e.getMessage());
				}
	            catch (Throwable t) {
	            	logger.error("Failed delivering transaction", t);
	            	transactionErrorSetter.accept(current.id, t.toString());
	    		}

				node.releaseWhoWasWaitingFor(current.request);
			}
			catch (InterruptedException e) {
				return;
			}
		}
	}

	/**
	 * A request with the associated identifier that can be used to wait for its result.
	 */
	private static class RequestWithId {
		private final TransactionRequest<?> request;

		/**
		 * The id that can be used to wait for the result of the request.
		 * This is chosen at the moment of submitting the request and used
		 * when it is fully executed, to signal who might be waiting for the result.
		 */
		private final String id;

		private RequestWithId(TransactionRequest<?> request, String id) {
			this.request = request;
			this.id = id;
		}

		@Override
		public String toString() {
			return id + ": " + request.getClass().getName();
		}
	}
}