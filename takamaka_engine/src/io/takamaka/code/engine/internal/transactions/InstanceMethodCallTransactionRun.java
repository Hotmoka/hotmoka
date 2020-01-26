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
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.takamaka.code.engine.Node;
import io.takamaka.code.engine.SideEffectsInViewMethodException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;
import io.takamaka.code.engine.internal.executors.InstanceMethodExecutor;

public class InstanceMethodCallTransactionRun extends AbstractTransactionRun<InstanceMethodCallTransactionRequest, MethodCallTransactionResponse> {

	public InstanceMethodCallTransactionRun(InstanceMethodCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node, request.gas);
	}

	@Override
	protected MethodCallTransactionResponse computeResponse() throws Exception {
		try (EngineClassLoaderImpl classLoader = new EngineClassLoaderImpl(request.classpath, this)) {
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

				InstanceMethodExecutor executor = new InstanceMethodExecutor(this, request.method, deserializedCaller, request.receiver, request.getActuals());
				executor.start();
				executor.join();

				if (executor.exception instanceof InvocationTargetException) {
					MethodCallTransactionResponse response = new MethodCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(classLoader::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					chargeForStorage(sizeCalculator.sizeOf(response));
					increaseBalance(deserializedCaller);
					return new MethodCallTransactionExceptionResponse((Exception) executor.exception.getCause(), executor.updates(), events.stream().map(classLoader::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				}

				if (executor.exception != null)
					throw executor.exception;

				if (executor.isViewMethod && !executor.onlyAffectedBalanceOf(deserializedCaller))
					throw new SideEffectsInViewMethodException((MethodSignature) executor.methodOrConstructor);

				if (executor.isVoidMethod) {
					MethodCallTransactionResponse response = new VoidMethodCallTransactionSuccessfulResponse(executor.updates(), events.stream().map(classLoader::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					chargeForStorage(sizeCalculator.sizeOf(response));
					increaseBalance(deserializedCaller);
					return new VoidMethodCallTransactionSuccessfulResponse(executor.updates(), events.stream().map(classLoader::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				}
				else {
					MethodCallTransactionResponse response = new MethodCallTransactionSuccessfulResponse
						(serializer.serialize(executor.result), executor.updates(), events.stream().map(classLoader::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
					chargeForStorage(sizeCalculator.sizeOf(response));
					increaseBalance(deserializedCaller);
					return new MethodCallTransactionSuccessfulResponse
						(serializer.serialize(executor.result), executor.updates(), events.stream().map(classLoader::getStorageReferenceOf), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				}
			}
			catch (Throwable t) {
				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
				BigInteger gasConsumedForPenalty = request.gas.subtract(gasConsumedForCPU).subtract(gasConsumedForRAM).subtract(gasConsumedForStorage);
				return new MethodCallTransactionFailedResponse(wrapAsTransactionException(t, "Failed transaction"), balanceUpdateInCaseOfFailure, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
			}
		}
	}
}