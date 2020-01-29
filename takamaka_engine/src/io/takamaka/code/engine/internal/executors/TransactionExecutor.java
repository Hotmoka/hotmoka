package io.takamaka.code.engine.internal.executors;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.takamaka.code.engine.internal.transactions.AbstractTransactionRun;

/**
 * The thread that executes a constructor or method of a Takamaka object. It creates the class loader
 * from the class path and deserializes receiver and actuals (if any). It then calls the code and serializes
 * the resulting value back (if any).
 */
public abstract class TransactionExecutor<Request extends TransactionRequest<Response>, Response extends TransactionResponse> extends Thread {

	/**
	 * The engine for which code is being executed.
	 */
	protected final AbstractTransactionRun<Request, Response> run;

	/**
	 * Builds the executor of a transaction.
	 * 
	 * @param run the engine for which code is being executed
	 * @throws TransactionException 
	 */
	protected TransactionExecutor(AbstractTransactionRun<Request, Response> run) {
		this.run = run;
	}

	/**
	 * Wraps the given throwable in a {@link io.hotmoka.beans.TransactionException}, if it not
	 * already an instance of that exception.
	 * 
	 * @param t the throwable to wrap
	 * @param message the message used for the {@link io.hotmoka.beans.TransactionException}, if wrapping occurs
	 * @return the wrapped or original exception
	 */
	protected final static TransactionException wrapAsTransactionException(Throwable t, String message) {
		return t instanceof TransactionException ? (TransactionException) t : new TransactionException(message, t);
	}
}