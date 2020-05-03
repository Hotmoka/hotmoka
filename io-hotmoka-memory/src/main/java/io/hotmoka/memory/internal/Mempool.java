package io.hotmoka.memory.internal;

import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.GuardedBy;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.takamaka.code.engine.ResponseBuilder;

public class Mempool {
	public final static int MAX_CAPACITY = 200_000;
	private final static Logger logger = LoggerFactory.getLogger(Mempool.class);

	private final BlockingQueue<RequestWithId> mempool = new LinkedBlockingDeque<>(MAX_CAPACITY);
	private final BlockingQueue<RequestWithId> checkedMempool = new LinkedBlockingDeque<>(MAX_CAPACITY);
	private final MemoryBlockchainImpl node;
	private final Object idLock = new Object();

	@GuardedBy("idLock")
	private BigInteger id;

	private final Thread checker;
	private final Thread deliverer;

	/**
	 * Builds a mempool.
	 * 
	 * @param node
	 */
	Mempool(MemoryBlockchainImpl node) {
		this.node = node;
		this.id = BigInteger.ZERO;
		this.checker = new Thread(this::check);
		this.checker.start();
		this.deliverer = new Thread(this::deliver);
		this.deliverer.start();
	}

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

	public void stop() {
		checker.interrupt();
		deliverer.interrupt();
	}

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
					node.setTransactionErrorFor(current.id, e.getMessage());
					node.releaseWhoWasWaitingFor(current.request);
				}
	            catch (Throwable t) {
	            	logger.error("Failed to check transaction request", t);
	            	node.setTransactionErrorFor(current.id, t.toString());
	            	node.releaseWhoWasWaitingFor(current.request);
	    		}
			}
			catch (InterruptedException e) {
				return;
			}
		}
	}

	private void deliver() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				RequestWithId current = checkedMempool.take();

				try {
					ResponseBuilder<?,?> builder = node.checkTransaction(current.request);
					TransactionReference next = node.nextAndIncrement();
					node.deliverTransaction(builder, next);
					node.setTransactionReferenceFor(current.id, next);
				}
				catch (TransactionRejectedException e) {
					logger.info("Failed delivering transaction", e);
					node.setTransactionErrorFor(current.id, e.getMessage());
				}
	            catch (Throwable t) {
	            	logger.error("Failed delivering transaction", t);
	            	node.setTransactionErrorFor(current.id, t.toString());
	    		}

				node.releaseWhoWasWaitingFor(current.request);
			}
			catch (InterruptedException e) {
				return;
			}
		}
	}

	private static class RequestWithId {
		private final TransactionRequest<?> request;
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