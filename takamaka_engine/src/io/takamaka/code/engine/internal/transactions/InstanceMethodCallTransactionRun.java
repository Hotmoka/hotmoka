package io.takamaka.code.engine.internal.transactions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.nodes.Node;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.SideEffectsInViewMethodException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;

public class InstanceMethodCallTransactionRun extends MethodCallTransactionRun<InstanceMethodCallTransactionRequest> {

	/**
	 * The deserialized receiver the call.
	 */
	public Object deserializedReceiver;

	public InstanceMethodCallTransactionRun(InstanceMethodCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException, IllegalTransactionRequestException {
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
		try {
			this.deserializedCaller = deserializer.deserialize(request.caller);
			this.deserializedReceiver = deserializer.deserialize(request.receiver);
			this.deserializedActuals = request.actuals().map(deserializer::deserialize).toArray(Object[]::new);
			checkIsExternallyOwned(deserializedCaller);
			// we sell all gas first: what remains will be paid back at the end;
			// if the caller has not enough to pay for the whole gas, the transaction won't be executed
			balanceUpdateInCaseOfFailure = checkMinimalGas(request, deserializedCaller);
			chargeForCPU(node.getGasCostModel().cpuBaseTransactionCost());
			chargeForStorage(sizeCalculator.sizeOf(request));
		}
		catch (IllegalTransactionRequestException e) {
			throw e;
		}
		catch (Throwable t) {
			throw new IllegalTransactionRequestException(t);
		}

		try {
			Thread executor = new Thread(this::run);
			executor.start();
			executor.join();

			if (exception instanceof InvocationTargetException) {
				MethodCallTransactionResponse response = new MethodCallTransactionExceptionResponse((Exception) exception.getCause(), updates(), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				chargeForStorage(sizeCalculator.sizeOf(response));
				increaseBalance(deserializedCaller);
				return new MethodCallTransactionExceptionResponse((Exception) exception.getCause(), updates(), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			}

			if (exception != null)
				throw exception;

			if (isViewMethod && !onlyAffectedBalanceOf())
				throw new SideEffectsInViewMethodException(method);

			if (isVoidMethod) {
				MethodCallTransactionResponse response = new VoidMethodCallTransactionSuccessfulResponse(updates(), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				chargeForStorage(sizeCalculator.sizeOf(response));
				increaseBalance(deserializedCaller);
				return new VoidMethodCallTransactionSuccessfulResponse(updates(), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			}
			else {
				MethodCallTransactionResponse response = new MethodCallTransactionSuccessfulResponse
						(serializer.serialize(result), updates(), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				chargeForStorage(sizeCalculator.sizeOf(response));
				increaseBalance(deserializedCaller);
				return new MethodCallTransactionSuccessfulResponse
						(serializer.serialize(result), updates(), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			}
		}
		catch (IllegalTransactionRequestException e) {
			throw e;
		}
		catch (Throwable t) {
			// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
			BigInteger gasConsumedForPenalty = request.gas.subtract(gasConsumedForCPU).subtract(gasConsumedForRAM).subtract(gasConsumedForStorage);
			return new MethodCallTransactionFailedResponse(wrapAsTransactionException(t, "Failed transaction"), balanceUpdateInCaseOfFailure, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
		}
	}

	private void run() {
		try {
			Method methodJVM;
			Object[] deserializedActuals;

			try {
				// we first try to call the method with exactly the parameter types explicitly provided
				methodJVM = getMethod();
				deserializedActuals = this.deserializedActuals;
			}
			catch (NoSuchMethodException e) {
				// if not found, we try to add the trailing types that characterize the @Entry methods
				try {
					methodJVM = getEntryMethod();
					deserializedActuals = addExtraActualsForEntry();
				}
				catch (NoSuchMethodException ee) {
					throw e; // the message must be relative to the method as the user sees it
				}
			}

			if (Modifier.isStatic(methodJVM.getModifiers()))
				throw new NoSuchMethodException("cannot call a static method: use addStaticMethodCallTransaction instead");

			ensureWhiteListingOf(methodJVM, deserializedActuals);

			isVoidMethod = methodJVM.getReturnType() == void.class;
			isViewMethod = hasAnnotation(methodJVM, Constants.VIEW_NAME);
			if (hasAnnotation(methodJVM, Constants.RED_PAYABLE_NAME))
				checkIsRedGreenExternallyOwned(deserializedCaller);

			try {
				result = methodJVM.invoke(deserializedReceiver, deserializedActuals);
			}
			catch (InvocationTargetException e) {
				exception = unwrapInvocationException(e, methodJVM);
			}
		}
		catch (Throwable t) {
			exception = t;
		}
	}
}