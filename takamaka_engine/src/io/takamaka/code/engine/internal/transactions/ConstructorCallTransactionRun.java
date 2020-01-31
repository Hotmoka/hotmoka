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
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;
import io.takamaka.code.engine.internal.executors.ConstructorExecutor;

public class ConstructorCallTransactionRun extends CodeCallTransactionRun<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> {
	public final CodeSignature constructor;

	public ConstructorCallTransactionRun(ConstructorCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException, IllegalTransactionRequestException {
		super(request, current, node);

		this.constructor = request.constructor;

		try (EngineClassLoaderImpl classLoader = new EngineClassLoaderImpl(request.classpath, this)) {
			this.classLoader = classLoader;
			this.response = computeResponse();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "cannot complete the transaction");
		}
	}

	private ConstructorCallTransactionResponse computeResponse() throws Exception {
		ConstructorExecutor executor = null;

		try {
			this.deserializedCaller = deserializer.deserialize(request.caller);
			this.deserializedActuals = request.actuals().map(deserializer::deserialize).toArray(Object[]::new);
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "cannot complete the transaction");
		}

		try {
			executor = new ConstructorExecutor(this);
			executor.start();
			executor.join();

			if (exception instanceof InvocationTargetException) {
				ConstructorCallTransactionResponse response = new ConstructorCallTransactionExceptionResponse((Exception) exception.getCause(), updates(executor), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				chargeForStorage(sizeCalculator.sizeOf(response));
				increaseBalance(deserializedCaller);
				return new ConstructorCallTransactionExceptionResponse((Exception) exception.getCause(), updates(executor), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			}

			if (exception != null)
				throw exception;

			ConstructorCallTransactionResponse response = new ConstructorCallTransactionSuccessfulResponse
					((StorageReference) serializer.serialize(result), updates(executor), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			chargeForStorage(sizeCalculator.sizeOf(response));
			increaseBalance(deserializedCaller);
			return new ConstructorCallTransactionSuccessfulResponse
					((StorageReference) serializer.serialize(result), updates(executor), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
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

	@Override
	public final CodeSignature getMethodOrConstructor() {
		return constructor;
	}
}