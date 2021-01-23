package io.takamaka.code.engine.internal.transactions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.ViewResponseBuilder;
import io.takamaka.code.engine.internal.NodeInternal;

/**
 * The builder of the response for a transaction that executes a static method of Takamaka code.
 */
public class StaticMethodCallResponseBuilder extends MethodCallResponseBuilder<StaticMethodCallTransactionRequest> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public StaticMethodCallResponseBuilder(TransactionReference reference, StaticMethodCallTransactionRequest request, NodeInternal node) throws TransactionRejectedException {
		super(reference, request, node);
	}

	@Override
	public MethodCallTransactionResponse getResponse() throws TransactionRejectedException {
		return new ResponseCreator().create();
	}

	private class ResponseCreator extends MethodCallResponseBuilder<StaticMethodCallTransactionRequest>.ResponseCreator {

		/**
		 * The deserialized actual arguments of the call.
		 */
		private Object[] deserializedActuals;

		private ResponseCreator() throws TransactionRejectedException {
		}

		@Override
		protected MethodCallTransactionResponse body() {
			try {
				init();
				this.deserializedActuals = request.actuals().map(deserializer::deserialize).toArray(Object[]::new);

				Method methodJVM = getMethod();
				boolean isView = hasAnnotation(methodJVM, Constants.VIEW_NAME);
				validateCallee(methodJVM, isView);
				ensureWhiteListingOf(methodJVM, deserializedActuals);

				Object result;
				try {
					result = methodJVM.invoke(null, deserializedActuals); // no receiver
				}
				catch (InvocationTargetException e) {
					Throwable cause = e.getCause();
					if (isCheckedForThrowsExceptions(cause, methodJVM)) {
						viewMustBeSatisfied(isView, null);
						chargeGasForStorageOf(new MethodCallTransactionExceptionResponse(cause.getClass().getName(), cause.getMessage(), where(cause), false, updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
						refundPayerForAllRemainingGas();
						sendAllConsumedGasToValidators();
						return new MethodCallTransactionExceptionResponse(cause.getClass().getName(), cause.getMessage(), where(cause), false, updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					}
					else
						throw cause;
				}

				viewMustBeSatisfied(isView, result);

				if (methodJVM.getReturnType() == void.class) {
					chargeGasForStorageOf(new VoidMethodCallTransactionSuccessfulResponse(false, updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
					refundPayerForAllRemainingGas();
					sendAllConsumedGasToValidators();
					return new VoidMethodCallTransactionSuccessfulResponse(false, updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
				else {
					chargeGasForStorageOf(new MethodCallTransactionSuccessfulResponse(serializer.serialize(result), false, updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
					refundPayerForAllRemainingGas();
					sendAllConsumedGasToValidators();
					return new MethodCallTransactionSuccessfulResponse(serializer.serialize(result), false, updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
			}
			catch (Throwable t) {
				sendAllConsumedGasToValidatorsIncludingPenalty();
				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller or validators
				return new MethodCallTransactionFailedResponse(t.getClass().getName(), t.getMessage(), where(t), false, updatesToBalanceOrNonceOfCallerOrValidators(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
			}
		}

		@Override
		protected final Stream<Object> getDeserializedActuals() {
			return Stream.of(deserializedActuals);
		}

		/**
		 * Checks that the called method respects the expected constraints.
		 * 
		 * @param methodJVM the method
		 * @param isView true if the method is annotated as view
		 * @throws NoSuchMethodException if the constraints are not satisfied
		 */
		private void validateCallee(Method methodJVM, boolean isView) throws NoSuchMethodException {
			if (!Modifier.isStatic(methodJVM.getModifiers()))
				throw new NoSuchMethodException("cannot call an instance method");

			if (!isView && StaticMethodCallResponseBuilder.this instanceof ViewResponseBuilder)
				throw new NoSuchMethodException("cannot call a method not annotated as @View");
		}
	}
}