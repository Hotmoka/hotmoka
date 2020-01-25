package io.takamaka.code.engine.internal.transactions;

import java.util.concurrent.Callable;

import io.hotmoka.beans.TransactionException;
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

	/**
	 * Calls the given callable. If if throws an exception, it wraps into into a {@link io.hotmoka.beans.TransactionException}.
	 * 
	 * @param what the callable
	 * @return the result of the callable
	 * @throws TransactionException the wrapped exception
	 */
	protected final static <T> T wrapInCaseOfException(Callable<T> what) throws TransactionException {
		try {
			return what.call();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "Cannot complete the transaction");
		}
	}

	/**
	 * Wraps the given throwable in a {@link io.hotmoka.beans.TransactionException}, if it not
	 * already an instance of that exception.
	 * 
	 * @param t the throwable to wrap
	 * @param message the message used for the {@link io.hotmoka.beans.TransactionException}, if wrapping occurs
	 * @return the wrapped or original exception
	 */
	private static TransactionException wrapAsTransactionException(Throwable t, String message) {
		return t instanceof TransactionException ? (TransactionException) t : new TransactionException(message, t);
	}
}