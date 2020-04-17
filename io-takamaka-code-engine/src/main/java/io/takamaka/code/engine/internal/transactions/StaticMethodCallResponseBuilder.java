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
import io.hotmoka.nodes.SideEffectsInViewMethodException;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.AbstractNode;

/**
 * The builder of the response for a transaction that executes a static method of Takamaka code.
 */
public class StaticMethodCallResponseBuilder extends MethodCallResponseBuilder<StaticMethodCallTransactionRequest> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public StaticMethodCallResponseBuilder(StaticMethodCallTransactionRequest request, AbstractNode node) throws TransactionRejectedException {
		super(request, node);
	}

	@Override
	public final MethodCallTransactionResponse build(TransactionReference current) throws TransactionRejectedException {
		try {
			return new ResponseCreator(current).response;
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	private class ResponseCreator extends MethodCallResponseBuilder<StaticMethodCallTransactionRequest>.ResponseCreator {

		/**
		 * The deserialized actual arguments of the call.
		 */
		private Object[] deserializedActuals;

		/**
		 * The response that is created.
		 */
		private final MethodCallTransactionResponse response;

		private ResponseCreator(TransactionReference current) throws Throwable {
			super(current);

			MethodCallTransactionResponse response = null;

			try {
				// we perform deserialization in a thread, since enums passed as parameters
				// would trigger the execution of their static initializer, which will charge gas
				DeserializerThread deserializerThread = new DeserializerThread(request);
				deserializerThread.go();
				this.deserializedActuals = deserializerThread.deserializedActuals;

				formalsAndActualsMustMatch();

				Method methodJVM = getMethod();
				validateCallee(methodJVM);
				ensureWhiteListingOf(methodJVM, deserializedActuals);

				MethodThread thread = new MethodThread(methodJVM, deserializedActuals);
				try {
					thread.go();
				}
				catch (InvocationTargetException e) {
					Throwable cause = e.getCause();
					if (isCheckedForThrowsExceptions(cause, methodJVM)) {
						chargeGasForStorageOf(new MethodCallTransactionExceptionResponse(cause.getClass().getName(), cause.getMessage(), where(cause), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
						payBackAllRemainingGasToCaller();
						response = new MethodCallTransactionExceptionResponse(cause.getClass().getName(), cause.getMessage(), where(cause), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					}
					else
						throw cause;
				}

				if (response == null) {
					if (hasAnnotation(methodJVM, Constants.VIEW_NAME) && !onlyAffectedBalanceOrNonceOfCaller(thread.result))
						throw new SideEffectsInViewMethodException(request.method);

					if (methodJVM.getReturnType() == void.class) {
						chargeGasForStorageOf(new VoidMethodCallTransactionSuccessfulResponse(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
						payBackAllRemainingGasToCaller();
						response = new VoidMethodCallTransactionSuccessfulResponse(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					}
					else {
						chargeGasForStorageOf(new MethodCallTransactionSuccessfulResponse(serializer.serialize(thread.result), updates(thread.result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
						payBackAllRemainingGasToCaller();
						response = new MethodCallTransactionSuccessfulResponse(serializer.serialize(thread.result), updates(thread.result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					}
				}
			}
			catch (Throwable t) {
				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
				response = new MethodCallTransactionFailedResponse(t.getClass().getName(), t.getMessage(), where(t), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
			}

			this.response = response;
		}

		@Override
		protected final Stream<Object> getDeserializedActuals() {
			return Stream.of(deserializedActuals);
		}

		/**
		 * Checks that the called method respects the expected constraints.
		 * 
		 * @param methodJVM the method
		 * @throws NoSuchMethodException if the constraints are not satisfied
		 */
		private void validateCallee(Method methodJVM) throws NoSuchMethodException {
			if (!Modifier.isStatic(methodJVM.getModifiers()))
				throw new NoSuchMethodException("cannot call an instance method");

			if (StaticMethodCallResponseBuilder.this instanceof ViewResponseBuilder && !hasAnnotation(methodJVM, Constants.VIEW_NAME))
				throw new NoSuchMethodException("cannot call a method not annotated as @View");
		}

		/**
		 * The thread that deserializes the caller, the receiver and the actual parameters.
		 * This must be done inside a thread so that static initializers
		 * are run with an associated {@code io.takamaka.code.engine.internal.Runtime} object.
		 */
		private class DeserializerThread extends TakamakaThread {
			private final StaticMethodCallTransactionRequest request;

			/**
			 * The deserialized actual arguments of the call.
			 */
			private Object[] deserializedActuals;

			private DeserializerThread(StaticMethodCallTransactionRequest request) {
				this.request = request;
			}

			@Override
			protected void body() throws Exception {
				this.deserializedActuals = request.actuals().map(deserializer::deserialize).toArray(Object[]::new);
			}
		}

		/**
		 * The thread that runs the method.
		 */
		private class MethodThread extends TakamakaThread {
			private Object result;
			private final Method methodJVM;
			private final Object[] deserializedActuals;

			private MethodThread(Method methodJVM, Object[] deserializedActuals) {
				this.methodJVM = methodJVM;
				this.deserializedActuals = deserializedActuals;
			}

			@Override
			protected void body() throws Exception {
				result = methodJVM.invoke(null, deserializedActuals); // no receiver
			}
		}
	}
}