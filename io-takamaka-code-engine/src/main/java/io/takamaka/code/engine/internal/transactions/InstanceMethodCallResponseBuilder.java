package io.takamaka.code.engine.internal.transactions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.SideEffectsInViewMethodException;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.internal.EngineClassLoader;

/**
 * The builder of the response of a transaction that executes an instance method of Takamaka code.
 */
public class InstanceMethodCallResponseBuilder extends MethodCallResponseBuilder<InstanceMethodCallTransactionRequest> {

	/**
	 * The class loader of the transaction.
	 */
	private final EngineClassLoader classLoader;

	/**
	 * The deserialized caller.
	 */
	private final Object deserializedCaller;

	/**
	 * The deserialized receiver the call.
	 */
	private Object deserializedReceiver;

	/**
	 * The deserialized actual arguments of the call.
	 */
	private Object[] deserializedActuals;

	/**
	 * Creates the builder of the response.
	 * 
	 * @param request the request of the transaction
	 * @param current the reference that must be used for the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public InstanceMethodCallResponseBuilder(InstanceMethodCallTransactionRequest request, TransactionReference current, Node node) throws TransactionRejectedException {
		super(request, current, node);

		try {
			this.classLoader = new EngineClassLoader(request.classpath, this);
			this.deserializedCaller = deserializer.deserialize(request.caller);
			callerMustBeExternallyOwnedAccount();
			callerAndRequestMustAgreeOnNonce();
			callerMustBeAbleToPayForAllGas();
			chargeGasForCPU(gasCostModel.cpuBaseTransactionCost());
			chargeGasForStorageOfRequest();
			remainingGasMustBeEnoughForStoringFailedResponse();

			sellAllGasToCaller();
			increaseNonceOfCaller();
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	public MethodCallTransactionResponse build() throws TransactionRejectedException {
		try {
			sellAllGasToCaller();
			increaseNonceOfCaller();

			try {
				// we perform deserialization in a thread, since enums passed as parameters
				// would trigger the execution of their static initializer, which will charge gas
				DeserializerThread deserializerThread = new DeserializerThread(request);
				deserializerThread.go();
				this.deserializedReceiver = deserializerThread.deserializedReceiver;
				this.deserializedActuals = deserializerThread.deserializedActuals;

				formalsAndActualsMustMatch();

				Object[] deserializedActuals;
				Method methodJVM;

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

				validateCallee(methodJVM);
				ensureWhiteListingOf(methodJVM, deserializedActuals);

				if (hasAnnotation(methodJVM, Constants.RED_PAYABLE_NAME))
					callerMustBeRedGreenExternallyOwnedAccount();

				MethodThread thread = new MethodThread(methodJVM, deserializedActuals);
				try {
					thread.go();
				}
				catch (InvocationTargetException e) {
					Throwable cause = e.getCause();
					if (isCheckedForThrowsExceptions(cause, methodJVM)) {
						chargeGasForStorage(new MethodCallTransactionExceptionResponse(cause.getClass().getName(), cause.getMessage(), where(cause), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
						payBackAllRemainingGasToCaller();
						return new MethodCallTransactionExceptionResponse(cause.getClass().getName(), cause.getMessage(), where(cause), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					}
					else
						throw cause;
				}

				if (hasAnnotation(methodJVM, Constants.VIEW_NAME) && !onlyAffectedBalanceOrNonceOfCaller(thread.result))
					throw new SideEffectsInViewMethodException(request.method);

				if (methodJVM.getReturnType() == void.class) {
					chargeGasForStorage(new VoidMethodCallTransactionSuccessfulResponse(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
					payBackAllRemainingGasToCaller();
					return new VoidMethodCallTransactionSuccessfulResponse(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
				else {
					chargeGasForStorage(new MethodCallTransactionSuccessfulResponse(serializer.serialize(thread.result), updates(thread.result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
					payBackAllRemainingGasToCaller();
					return new MethodCallTransactionSuccessfulResponse(serializer.serialize(thread.result), updates(thread.result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
			}
			catch (Throwable t) {
				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
				return new MethodCallTransactionFailedResponse(t.getClass().getName(), t.getMessage(), where(t), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
			}
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
	protected void validateCallee(Method methodJVM) throws NoSuchMethodException {
		if (Modifier.isStatic(methodJVM.getModifiers()))
			throw new NoSuchMethodException("cannot call a static method");
	}

	@Override
	public final EngineClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	protected void scanPotentiallyAffectedObjects(Consumer<Object> consumer) {
		super.scanPotentiallyAffectedObjects(consumer);

		// the receiver is accessible from environment of the caller
		consumer.accept(deserializedReceiver);
	}

	@Override
	protected void ensureWhiteListingOf(Method executable, Object[] actuals) throws ClassNotFoundException {
		super.ensureWhiteListingOf(executable, actuals);

		// we check the annotations on the receiver as well
		Optional<Method> model = classLoader.getWhiteListingWizard().whiteListingModelOf(executable);
		if (model.isPresent() && !Modifier.isStatic(executable.getModifiers()))
			checkWhiteListingProofObligations(model.get().getName(), deserializedReceiver, model.get().getAnnotations());
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
	 * Adds to the actual parameters the implicit actuals that are passed
	 * to {@link io.takamaka.code.lang.Entry} methods or constructors. They are the caller of
	 * the entry and {@code null} for the dummy argument.
	 * 
	 * @return the resulting actual parameters
	 */
	private Object[] addExtraActualsForEntry() {
		int al = deserializedActuals.length;
		Object[] result = new Object[al + 2];
		System.arraycopy(deserializedActuals, 0, result, 0, al);
		result[al] = getDeserializedCaller();
		result[al + 1] = null; // Dummy is not used

		return result;
	}

	/**
	 * Resolves the method that must be called, assuming that it is an entry.
	 * 
	 * @return the method
	 * @throws NoSuchMethodException if the method could not be found
	 * @throws SecurityException if the method could not be accessed
	 * @throws ClassNotFoundException if the class of the method or of some parameter or return type cannot be found
	 */
	private Method getEntryMethod() throws NoSuchMethodException, SecurityException, ClassNotFoundException {
		MethodSignature method = request.method;
		Class<?> returnType = method instanceof NonVoidMethodSignature ? storageTypeToClass.toClass(((NonVoidMethodSignature) method).returnType) : void.class;
		Class<?>[] argTypes = formalsAsClassForEntry();

		return classLoader.resolveMethod(method.definingClass.name, method.methodName, argTypes, returnType)
			.orElseThrow(() -> new NoSuchMethodException(method.toString()));
	}

	/**
	 * The thread that deserializes the caller, the receiver and the actual parameters.
	 * This must be done inside a thread so that static initializers
	 * are run with an associated {@code io.takamaka.code.engine.internal.Runtime} object.
	 */
	private class DeserializerThread extends TakamakaThread {
		private final InstanceMethodCallTransactionRequest request;

		/**
		 * The deserialized receiver the call.
		 */
		private Object deserializedReceiver;

		/**
		 * The deserialized actual arguments of the call.
		 */
		private Object[] deserializedActuals;

		private DeserializerThread(InstanceMethodCallTransactionRequest request) {
			this.request = request;
		}

		@Override
		protected void body() throws Exception {
			deserializedReceiver = deserializer.deserialize(request.receiver);
			deserializedActuals = request.actuals().map(deserializer::deserialize).toArray(Object[]::new);
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
			result = methodJVM.invoke(deserializedReceiver, deserializedActuals);
		}
	}
}