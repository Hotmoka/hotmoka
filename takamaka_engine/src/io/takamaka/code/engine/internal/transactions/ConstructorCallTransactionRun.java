package io.takamaka.code.engine.internal.transactions;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;
import io.takamaka.code.engine.internal.executors.ConstructorExecutor;

public class ConstructorCallTransactionRun extends NonInitialTransactionRun<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> {

	public ConstructorCallTransactionRun(ConstructorCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException, IllegalTransactionRequestException {
		super(request, current, node);
	}

	@Override
	protected EngineClassLoaderImpl mkClassLoader() throws Exception {
		return new EngineClassLoaderImpl(request.classpath, this);
	}

	@Override
	protected ConstructorCallTransactionResponse computeResponse() throws Exception {
		ConstructorExecutor executor = null;
		try {
			executor = new ConstructorExecutor(this, request.constructor, request.actuals());
			executor.start();
			executor.join();

			if (executor.exception instanceof InvocationTargetException) {
				ConstructorCallTransactionResponse response = new ConstructorCallTransactionExceptionResponse((Exception) executor.exception.getCause(), updates(executor), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				chargeForStorage(sizeCalculator.sizeOf(response));
				increaseBalance(executor.deserializedCaller);
				return new ConstructorCallTransactionExceptionResponse((Exception) executor.exception.getCause(), updates(executor), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			}

			if (executor.exception != null)
				throw executor.exception;

			ConstructorCallTransactionResponse response = new ConstructorCallTransactionSuccessfulResponse
					((StorageReference) serializer.serialize(executor.result), updates(executor), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			chargeForStorage(sizeCalculator.sizeOf(response));
			increaseBalance(executor.deserializedCaller);
			return new ConstructorCallTransactionSuccessfulResponse
					((StorageReference) serializer.serialize(executor.result), updates(executor), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		catch (IllegalTransactionRequestException e) {
			throw e;
		}
		catch (Throwable t) {
			// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
			BigInteger gasConsumedForPenalty = request.gas.subtract(gasConsumedForCPU).subtract(gasConsumedForRAM).subtract(gasConsumedForStorage);
			return new ConstructorCallTransactionFailedResponse(wrapAsTransactionException(t, "Failed transaction"), executor.balanceUpdateInCaseOfFailure, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
		}
	}
}