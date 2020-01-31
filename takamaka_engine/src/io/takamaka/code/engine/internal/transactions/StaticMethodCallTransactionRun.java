package io.takamaka.code.engine.internal.transactions;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.SideEffectsInViewMethodException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;
import io.takamaka.code.engine.internal.executors.StaticMethodExecutor;

public class StaticMethodCallTransactionRun extends MethodCallTransactionRun<StaticMethodCallTransactionRequest> {

	public StaticMethodCallTransactionRun(StaticMethodCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException, IllegalTransactionRequestException {
		super(request, current, node);

		try (EngineClassLoaderImpl classLoader = new EngineClassLoaderImpl(request.classpath, this)) {
			this.classLoader = classLoader;
			this.response = computeResponse();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "cannot complete the transaction");
		}
	}

	private MethodCallTransactionResponse computeResponse() throws Exception {
		StaticMethodExecutor executor = null;

		try {
			this.deserializedCaller = deserializer.deserialize(request.caller);
			this.deserializedActuals = request.actuals().map(deserializer::deserialize).toArray(Object[]::new);
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "cannot complete the transaction");
		}

		try {
			executor = new StaticMethodExecutor(this);
			executor.start();
			executor.join();

			if (exception instanceof InvocationTargetException) {
				MethodCallTransactionResponse response = new MethodCallTransactionExceptionResponse((Exception) exception.getCause(), updates(executor), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				chargeForStorage(sizeCalculator.sizeOf(response));
				increaseBalance(deserializedCaller);
				return new MethodCallTransactionExceptionResponse((Exception) exception.getCause(), updates(executor), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			}

			if (exception != null)
				throw exception;

			if (isViewMethod && !onlyAffectedBalanceOf(executor))
				throw new SideEffectsInViewMethodException(method);

			if (isVoidMethod) {
				MethodCallTransactionResponse response = new VoidMethodCallTransactionSuccessfulResponse(updates(executor), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				chargeForStorage(sizeCalculator.sizeOf(response));
				increaseBalance(deserializedCaller);
				return new VoidMethodCallTransactionSuccessfulResponse(updates(executor), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			}
			else {
				MethodCallTransactionResponse response = new MethodCallTransactionSuccessfulResponse
						(serializer.serialize(result), updates(executor), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				chargeForStorage(sizeCalculator.sizeOf(response));
				increaseBalance(deserializedCaller);
				return new MethodCallTransactionSuccessfulResponse
						(serializer.serialize(result), updates(executor), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			}
		}
		catch (IllegalTransactionRequestException e) {
			throw e;
		}
		catch (Throwable t) {
			// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
			BigInteger gasConsumedForPenalty = request.gas.subtract(gasConsumedForCPU).subtract(gasConsumedForRAM).subtract(gasConsumedForStorage);
			return new MethodCallTransactionFailedResponse(wrapAsTransactionException(t, "Failed transaction"), executor.balanceUpdateInCaseOfFailure, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
		}
	}
}