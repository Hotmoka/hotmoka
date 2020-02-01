package io.takamaka.code.engine.internal.transactions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.hotmoka.nodes.Node;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.SideEffectsInViewMethodException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;

public class InstanceMethodCallTransactionRun extends MethodCallTransactionRun<InstanceMethodCallTransactionRequest> {

	/**
	 * The deserialized receiver the call.
	 */
	private final Object deserializedReceiver;

	private final EngineClassLoaderImpl classLoader;

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

	public InstanceMethodCallTransactionRun(InstanceMethodCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node);

		try (EngineClassLoaderImpl classLoader = new EngineClassLoaderImpl(request.classpath, this)) {
			this.classLoader = classLoader;
			this.deserializedCaller = deserializer.deserialize(request.caller);
			this.deserializedReceiver = deserializer.deserialize(request.receiver);
			this.deserializedActuals = request.actuals().map(deserializer::deserialize).toArray(Object[]::new);
			checkIsExternallyOwned(deserializedCaller);
			// we sell all gas first: what remains will be paid back at the end;
			// if the caller has not enough to pay for the whole gas, the transaction won't be executed
			UpdateOfBalance balanceUpdateInCaseOfFailure = checkMinimalGas(request, deserializedCaller);
			chargeForCPU(node.getGasCostModel().cpuBaseTransactionCost());
			chargeForStorage(request);
			MethodCallTransactionResponse response = null;
			Method methodJVM = null;

			try {
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
					throw new NoSuchMethodException("cannot call a static method");

				ensureWhiteListingOf(methodJVM, deserializedActuals);

				boolean isVoidMethod = methodJVM.getReturnType() == void.class;
				boolean isViewMethod = hasAnnotation(methodJVM, Constants.VIEW_NAME);
				if (hasAnnotation(methodJVM, Constants.RED_PAYABLE_NAME))
					checkIsRedGreenExternallyOwned(deserializedCaller);

				Object result = methodJVM.invoke(deserializedReceiver, deserializedActuals);

				if (isViewMethod && !onlyAffectedBalanceOfCaller(result))
					throw new SideEffectsInViewMethodException(method);

				if (isVoidMethod) {
					chargeForStorage(new VoidMethodCallTransactionSuccessfulResponse(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
					increaseBalance(deserializedCaller);
					response = new VoidMethodCallTransactionSuccessfulResponse(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
				else {
					chargeForStorage(new MethodCallTransactionSuccessfulResponse(serializer.serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
					increaseBalance(deserializedCaller);
					response = new MethodCallTransactionSuccessfulResponse(serializer.serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
			}
			catch (InvocationTargetException e) {
				if (isCheckedForThrowsExceptions(e, methodJVM)) {
					chargeForStorage(new MethodCallTransactionExceptionResponse((Exception) e.getCause(), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
					increaseBalance(deserializedCaller);
					response = new MethodCallTransactionExceptionResponse((Exception) e.getCause(), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
				else
					throw e.getCause();
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
	public final EngineClassLoaderImpl getClassLoader() {
		return classLoader;
	}

	@Override
	public final MethodCallTransactionResponse getResponse() {
		return response;
	}

	@Override
	protected void scanPotentiallyAffectedObjects(Consumer<Object> add) {
		super.scanPotentiallyAffectedObjects(add);

		// the receiver is accessible from environment of the caller
		add.accept(deserializedReceiver);
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
		Class<?> returnType = method instanceof NonVoidMethodSignature ? storageTypeToClass.toClass(((NonVoidMethodSignature) method).returnType) : void.class;
		Class<?>[] argTypes = formalsAsClassForEntry();

		return classLoader.resolveMethod(method.definingClass.name, method.methodName, argTypes, returnType)
			.orElseThrow(() -> new NoSuchMethodException(method.toString()));
	}
}