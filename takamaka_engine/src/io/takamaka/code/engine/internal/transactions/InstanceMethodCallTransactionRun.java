package io.takamaka.code.engine.internal.transactions;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.SideEffectsInViewMethodException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;
import io.takamaka.code.engine.internal.executors.InstanceMethodExecutor;

public class InstanceMethodCallTransactionRun extends NonInitialTransactionRun<InstanceMethodCallTransactionRequest, MethodCallTransactionResponse> {

	public InstanceMethodCallTransactionRun(InstanceMethodCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException, IllegalTransactionRequestException {
		super(request, current, node);
	}

	@Override
	protected EngineClassLoaderImpl mkClassLoader() throws Exception {
		return new EngineClassLoaderImpl(request.classpath, this);
	}

	@Override
	protected MethodCallTransactionResponse computeResponse() throws Exception {
		InstanceMethodExecutor executor = null;
		try {
			executor = new InstanceMethodExecutor(this, request.method, request.receiver, request.getActuals());
			executor.start();
			executor.join();

			if (executor.exception instanceof InvocationTargetException) {
				MethodCallTransactionResponse response = new MethodCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), executor.events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				chargeForStorage(sizeCalculator.sizeOf(response));
				increaseBalance(executor.deserializedCaller);
				return new MethodCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), executor.events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			}

			if (executor.exception != null)
				throw executor.exception;

			if (executor.isViewMethod && !executor.onlyAffectedBalanceOf(executor.deserializedCaller))
				throw new SideEffectsInViewMethodException((MethodSignature) executor.methodOrConstructor);

			if (executor.isVoidMethod) {
				MethodCallTransactionResponse response = new VoidMethodCallTransactionSuccessfulResponse(executor.updates(), executor.events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				chargeForStorage(sizeCalculator.sizeOf(response));
				increaseBalance(executor.deserializedCaller);
				return new VoidMethodCallTransactionSuccessfulResponse(executor.updates(), executor.events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			}
			else {
				MethodCallTransactionResponse response = new MethodCallTransactionSuccessfulResponse
						(serializer.serialize(executor.result), executor.updates(), executor.events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				chargeForStorage(sizeCalculator.sizeOf(response));
				increaseBalance(executor.deserializedCaller);
				return new MethodCallTransactionSuccessfulResponse
						(serializer.serialize(executor.result), executor.updates(), executor.events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
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