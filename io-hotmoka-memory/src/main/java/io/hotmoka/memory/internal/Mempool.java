package io.hotmoka.memory.internal;

import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.GuardedBy;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.takamaka.code.engine.ResponseBuilder;

public class Mempool {
	public final static int MAX_CAPACITY = 100_000;

	private final BlockingQueue<RequestWithId> mempool = new LinkedBlockingDeque<>(MAX_CAPACITY);
	private final AbstractMemoryBlockchain node;
	private final Object idLock = new Object();

	@GuardedBy("idLock")
	private BigInteger id;

	private final Thread worker;

	Mempool(AbstractMemoryBlockchain node) {
		this.node = node;
		this.id = BigInteger.ZERO;
		this.worker = new Thread(this::work);
		this.worker.start();
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
		worker.interrupt();
	}

	private void work() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				RequestWithId current = mempool.take();
				TransactionReference next = node.nextAndIncrement();

				try {
					ResponseBuilder<?,?> builder = node.checkTransaction(current.request);
					//System.out.println(current.request.getClass().getName());
					node.deliverTransaction(builder, next);
					node.setTransactionReferenceFor(current.id, next);
				}
				catch (TransactionRejectedException e) {
					node.setTransactionErrorFor(current.id, e.getMessage());
				}
	            catch (Throwable t) {
	            	node.setTransactionErrorFor(current.id, t.toString());
	    		}
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