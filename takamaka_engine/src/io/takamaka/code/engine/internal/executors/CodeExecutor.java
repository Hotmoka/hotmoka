package io.takamaka.code.engine.internal.executors;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.requests.CodeExecutionTransactionRequest;
import io.hotmoka.beans.responses.CodeExecutionTransactionResponse;
import io.takamaka.code.engine.internal.transactions.AbstractTransactionRun;
import io.takamaka.code.engine.internal.transactions.NonInitialTransactionRun;

/**
 * The thread that executes a constructor or method of a Takamaka object. It creates the class loader
 * from the class path and deserializes receiver and actuals (if any). It then calls the code and serializes
 * the resulting value back (if any).
 */
public abstract class CodeExecutor<Request extends CodeExecutionTransactionRequest<Response>, Response extends CodeExecutionTransactionResponse> extends Thread {

	/**
	 * The engine for which code is being executed.
	 */
	protected final AbstractTransactionRun<Request, Response> run;

	/**
	 * Builds the executor of a method or constructor.
	 * 
	 * @param run the engine for which code is being executed
	 * @param classLoader the class loader that must be used to find the classes during the execution of the method or constructor
	 * @throws TransactionException 
	 */
	protected CodeExecutor(NonInitialTransactionRun<Request, Response> run) throws TransactionException {
		this.run = run;
	}
}