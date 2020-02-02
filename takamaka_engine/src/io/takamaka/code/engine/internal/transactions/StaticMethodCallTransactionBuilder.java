package io.takamaka.code.engine.internal.transactions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.SideEffectsInViewMethodException;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.internal.EngineClassLoader;

public class StaticMethodCallTransactionBuilder extends MethodCallTransactionBuilder<StaticMethodCallTransactionRequest> {
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

	public StaticMethodCallTransactionBuilder(StaticMethodCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node);

		try (EngineClassLoader classLoader = new EngineClassLoader(request.classpath, this)) {
			this.classLoader = classLoader;

			// we perform deserialization in a thread, since enums passed as parameters
			// would trigger the execution of their static initializer, which will charge gas
			DeserializerThread deserializerThread = new DeserializerThread(request);
			deserializerThread.start();
			deserializerThread.join();
			if (deserializerThread.exception != null)
				throw deserializerThread.exception;

			this.deserializedCaller = deserializerThread.deserializedCaller;
			this.deserializedActuals = deserializerThread.deserializedActuals;

			checkIsExternallyOwned(deserializedCaller);
			// we sell all gas first: what remains will be paid back at the end;
			// if the caller has not enough to pay for the whole gas, the transaction won't be executed
			UpdateOfBalance balanceUpdateInCaseOfFailure = checkMinimalGas(request, deserializedCaller);
			chargeForCPU(node.getGasCostModel().cpuBaseTransactionCost());
			chargeForStorage(request);
			MethodCallTransactionResponse response = null;
			Method methodJVM = null;

			try {
				methodJVM = getMethod();

				if (!Modifier.isStatic(methodJVM.getModifiers()))
					throw new NoSuchMethodException("cannot call an instance method");

				ensureWhiteListingOf(methodJVM, deserializedActuals);

				boolean isVoidMethod = methodJVM.getReturnType() == void.class;
				boolean isViewMethod = hasAnnotation(methodJVM, Constants.VIEW_NAME);

				MethodThread thread = new MethodThread(methodJVM, deserializedActuals);
				thread.start();
				thread.join();
				if (thread.exception != null)
					if (thread.exception instanceof InvocationTargetException) {
						Throwable cause = thread.exception.getCause();
						if (isCheckedForThrowsExceptions(cause, methodJVM)) {
							chargeForStorage(new MethodCallTransactionExceptionResponse((Exception) cause, updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
							increaseBalance(deserializedCaller);
							response = new MethodCallTransactionExceptionResponse((Exception) cause, updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
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
						increaseBalance(deserializedCaller);
						response = new VoidMethodCallTransactionSuccessfulResponse(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					}
					else {
						chargeForStorage(new MethodCallTransactionSuccessfulResponse(serializer.serialize(thread.result), updates(thread.result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
						increaseBalance(deserializedCaller);
						response = new MethodCallTransactionSuccessfulResponse(serializer.serialize(thread.result), updates(thread.result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					}
				}
			}
			catch (Throwable t) {
				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
				response = new MethodCallTransactionFailedResponse(wrapAsTransactionException(t), balanceUpdateInCaseOfFailure, gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
			}

			this.response = response;
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t);
		}
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