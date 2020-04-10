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
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.SideEffectsInViewMethodException;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.internal.EngineClassLoader;

/**
 * Builds the creator of a transaction that executes a static method of Takamaka code.
 */
public class StaticMethodCallTransactionBuilder extends MethodCallTransactionBuilder<StaticMethodCallTransactionRequest> {

	/**
	 * The class loader of the transaction.
	 */
	private final EngineClassLoader classLoader;

	/**
	 * The deserialized caller.
	 */
	private final Object deserializedCaller;

	/**
	 * The deserialized actual arguments of the call.
	 */
	private final Object[] deserializedActuals;

	/**
	 * The response computed at the end of the transaction.
	 */
	private final MethodCallTransactionResponse response;

	/**
	 * Creates the builder of a transaction that executes a static method of Takamaka code.
	 * 
	 * @param request the request of the transaction
	 * @param current the reference that must be used for the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public StaticMethodCallTransactionBuilder(StaticMethodCallTransactionRequest request, TransactionReference current, Node node) throws TransactionRejectedException {
		super(request, current, node);

		try {
			this.classLoader = new EngineClassLoader(request.classpath, this);

			if (request.method.formals().count() != request.actuals().count())
				throw new IllegalArgumentException("argument count mismatch between formals and actuals");

			// we perform deserialization in a thread, since enums passed as parameters
			// would trigger the execution of their static initializer, which will charge gas
			DeserializerThread deserializerThread = new DeserializerThread(request);
			deserializerThread.start();
			deserializerThread.join();
			if (deserializerThread.exception != null)
				throw deserializerThread.exception;

			this.deserializedCaller = deserializerThread.deserializedCaller;
			this.deserializedActuals = deserializerThread.deserializedActuals;

			callerIsAnExternallyOwnedAccount();
			callerAndRequestAgreeOnNonce();
			sellAllGasToCaller();
			gasIsEnoughToPayForFailure();
			chargeForCPU(gasCostModel.cpuBaseTransactionCost());
			chargeForStorageOfRequest();
			MethodCallTransactionResponse response;

			try {
				Method methodJVM = getMethod();
				validateTarget(methodJVM);
				ensureWhiteListingOf(methodJVM);

				boolean isVoidMethod = methodJVM.getReturnType() == void.class;
				boolean isViewMethod = hasAnnotation(methodJVM, Constants.VIEW_NAME);

				MethodThread thread = new MethodThread(methodJVM, deserializedActuals);
				thread.start();
				thread.join();
				if (thread.exception != null)
					if (thread.exception instanceof InvocationTargetException) {
						Throwable cause = thread.exception.getCause();
						if (isCheckedForThrowsExceptions(cause, methodJVM)) {
							chargeForStorage(new MethodCallTransactionExceptionResponse(cause.getClass().getName(), cause.getMessage(), where(cause), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
							payBackRemainingGas();
							increaseNonceOfCaller();
							response = new MethodCallTransactionExceptionResponse(cause.getClass().getName(), cause.getMessage(), where(cause), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
						}
						else
							throw cause;
					}
					else
						throw thread.exception;
				else {
					if (isViewMethod && !onlyAffectedBalanceOfCaller(thread.result))
						throw new SideEffectsInViewMethodException(method);

					if (isVoidMethod) {
						chargeForStorage(new VoidMethodCallTransactionSuccessfulResponse(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
						payBackRemainingGas();
						response = new VoidMethodCallTransactionSuccessfulResponse(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					}
					else {
						chargeForStorage(new MethodCallTransactionSuccessfulResponse(serializer.serialize(thread.result), updates(thread.result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
						payBackRemainingGas();
						increaseNonceOfCaller();
						response = new MethodCallTransactionSuccessfulResponse(serializer.serialize(thread.result), updates(thread.result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					}
				}
			}
			catch (Throwable t) {
				increaseNonceOfCaller();
				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
				response = new MethodCallTransactionFailedResponse(t.getClass().getName(), t.getMessage(), where(t), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
			}

			this.response = response;
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	/**
	 * Checks that the called method respects the expected constraints.
	 * 
	 * @param methodJVM the method
	 * @throws NoSuchMethodException if the constraints are not satisfied
	 */
	protected void validateTarget(Method methodJVM) throws NoSuchMethodException {
		if (!Modifier.isStatic(methodJVM.getModifiers()))
			throw new NoSuchMethodException("cannot call an instance method");
	}

	@Override
	public final EngineClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public final MethodCallTransactionResponse getResponse() {
		return response;
	}

	@Override
	protected final Object getDeserializedCaller() {
		return deserializedCaller;
	}

	@Override
	protected final Stream<Object> getDeserializedActuals() {
		return Stream.of(deserializedActuals);
	}

	/**
	 * The thread that deserializes the caller, the receiver and the actual parameters.
	 * This must be done inside a thread so that static initializers
	 * are run with an associated {@code io.takamaka.code.engine.internal.Runtime} object.
	 */
	private class DeserializerThread extends TakamakaThread {
		private final StaticMethodCallTransactionRequest request;

		/**
		 * The deserialized caller.
		 */
		private Object deserializedCaller;

		/**
		 * The deserialized actual arguments of the call.
		 */
		private Object[] deserializedActuals;

		private DeserializerThread(StaticMethodCallTransactionRequest request) {
			this.request = request;
		}

		@Override
		protected void body() throws Exception {
			this.deserializedCaller = deserializer.deserialize(request.caller);
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