package io.takamaka.code.engine.internal.executors;

import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.requests.CodeExecutionTransactionRequest;
import io.hotmoka.beans.responses.CodeExecutionTransactionResponse;
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.internal.transactions.AbstractTransactionRun;
import io.takamaka.code.engine.internal.transactions.NonInitialTransactionRun;

/**
 * The thread that executes a constructor or method of a Takamaka object. It creates the class loader
 * from the class path and deserializes receiver and actuals (if any). It then calls the code and serializes
 * the resulting value back (if any).
 */
public abstract class CodeExecutor<Request extends CodeExecutionTransactionRequest<Response>, Response extends CodeExecutionTransactionResponse> extends Thread {
	public final UpdateOfBalance balanceUpdateInCaseOfFailure;

	/**
	 * The engine for which code is being executed.
	 */
	protected final AbstractTransactionRun<Request, Response> run;

	/**
	 * The deserialized caller.
	 */
	public final Object deserializedCaller;

	/**
	 * The deserialized receiver of a method call. This is {@code null} for static methods and constructors.
	 */
	public final Object deserializedReceiver; // it might be null

	/**
	 * The deserialized actual arguments of the call.
	 */
	public final Object[] deserializedActuals;

	/**
	 * Builds the executor of a method or constructor.
	 * 
	 * @param run the engine for which code is being executed
	 * @param classLoader the class loader that must be used to find the classes during the execution of the method or constructor
	 * @param receiver the receiver of the call, if any. This is {@code null} for constructors and static methods
	 * @param actuals the actuals provided to the method or constructor
	 * @throws TransactionException 
	 */
	protected CodeExecutor(NonInitialTransactionRun<Request, Response> run, StorageReference receiver, Stream<StorageValue> actuals) throws IllegalTransactionRequestException, TransactionException {
		try {
			this.run = run;
			this.deserializedCaller = run.deserializer.deserialize(run.request.caller);
			run.checkIsExternallyOwned(deserializedCaller);

			// we sell all gas first: what remains will be paid back at the end;
			// if the caller has not enough to pay for the whole gas, the transaction won't be executed
			this.balanceUpdateInCaseOfFailure = run.checkMinimalGas(run.request, deserializedCaller);
			run.chargeForCPU(run.node.getGasCostModel().cpuBaseTransactionCost());
			run.chargeForStorage(run.sizeCalculator.sizeOf(run.request));
			this.deserializedReceiver = receiver != null ? run.deserializer.deserialize(receiver) : null;
			this.deserializedActuals = actuals.map(run.deserializer::deserialize).toArray(Object[]::new);
		}
		catch (IllegalTransactionRequestException e) {
			throw e;
		}
		catch (Throwable t) {
			throw new IllegalTransactionRequestException(t);
		}
	}
}