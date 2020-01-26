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
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.hotmoka.beans.values.StorageReference;
import io.takamaka.code.engine.Node;
import io.takamaka.code.engine.internal.EngineClassLoader;
import io.takamaka.code.engine.internal.executors.CodeExecutor;
import io.takamaka.code.engine.internal.executors.ConstructorExecutor;

public class ConstructorCallTransactionRun extends AbstractTransactionRun<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> {

	public ConstructorCallTransactionRun(ConstructorCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node, request.gas);
	}

	@Override
	protected ConstructorCallTransactionResponse computeResponse() throws Exception {
		try (EngineClassLoader classLoader = new EngineClassLoader(request.classpath, this)) {
			this.classLoader = classLoader;
			Object deserializedCaller = deserializer.deserialize(request.caller);
			checkIsExternallyOwned(deserializedCaller);
			
			// we sell all gas first: what remains will be paid back at the end;
			// if the caller has not enough to pay for the whole gas, the transaction won't be executed
			UpdateOfBalance balanceUpdateInCaseOfFailure = checkMinimalGas(request, deserializedCaller);

			// before this line, an exception will abort the transaction and leave the blockchain unchanged;
			// after this line, the transaction can be added to the blockchain, possibly as a failed one

			try {
				chargeForCPU(node.getGasCostModel().cpuBaseTransactionCost());
				chargeForStorage(sizeCalculator.sizeOf(request));

				CodeExecutor executor = new ConstructorExecutor(this, request.constructor, deserializedCaller, request.actuals());
				executor.start();
				executor.join();

				if (executor.exception instanceof InvocationTargetException) {
					ConstructorCallTransactionResponse response = new ConstructorCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(event -> getStorageReferenceOf(event)), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					chargeForStorage(sizeCalculator.sizeOf(response));
					increaseBalance(deserializedCaller);
					return new ConstructorCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(this::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				}

				if (executor.exception != null)
					throw executor.exception;

				ConstructorCallTransactionResponse response = new ConstructorCallTransactionSuccessfulResponse
					((StorageReference) serializer.serialize(executor.result), executor.updates(), events.stream().map(this::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				chargeForStorage(sizeCalculator.sizeOf(response));
				increaseBalance(deserializedCaller);
				return new ConstructorCallTransactionSuccessfulResponse
					((StorageReference) serializer.serialize(executor.result), executor.updates(), events.stream().map(this::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			}
			catch (Throwable t) {
				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
				BigInteger gasConsumedForPenalty = request.gas.subtract(gasConsumedForCPU).subtract(gasConsumedForRAM).subtract(gasConsumedForStorage);
				return new ConstructorCallTransactionFailedResponse(wrapAsTransactionException(t, "Failed transaction"), balanceUpdateInCaseOfFailure, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
			}
		}
	}
}