package io.takamaka.code.engine.internal.transactions;

import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.takamaka.code.engine.Transaction;

/**
 * A transaction of HotMoka code: it is the execution of a
 * request, that led to a response.
 *
 * @param <R> the type of the response of this transaction
 */
public abstract class AbstractTransaction<R extends TransactionResponse> implements Transaction<R> {

	/**
	 * The request from which this transaction started.
	 */
	private final TransactionRequest<R> request;

	/**
	 * Builds a transaction from the given request.
	 * 
	 * @param request the request
	 */
	protected AbstractTransaction(TransactionRequest<R> request) {
		this.request = request;
	}

	@Override
	public final TransactionRequest<R> getRequest() {
		return request;
	}
}