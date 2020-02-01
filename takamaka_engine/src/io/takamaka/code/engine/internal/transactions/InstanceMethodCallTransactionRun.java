package io.takamaka.code.engine.internal.transactions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Consumer;

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
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.SideEffectsInViewMethodException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;

public class InstanceMethodCallTransactionRun extends MethodCallTransactionRun<InstanceMethodCallTransactionRequest> {

	/**
	 * The deserialized receiver the call.
	 */
	private final Object deserializedReceiver;

	private final EngineClassLoaderImpl classLoader;

	/**
	 * The response computed at the end of the transaction.
	 */
	private MethodCallTransactionResponse response; // TODO: make final

	public InstanceMethodCallTransactionRun(InstanceMethodCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node);

		try (EngineClassLoaderImpl classLoader = new EngineClassLoaderImpl(request.classpath, this)) {
			this.classLoader = classLoader;
			UpdateOfBalance balanceUpdateInCaseOfFailure;

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
				Throwable exception = null;
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

				if (exception instanceof InvocationTargetException) {
					MethodCallTransactionResponse response = new MethodCallTransactionExceptionResponse((Exception) exception.getCause(), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					chargeForStorage(sizeCalculator.sizeOf(response));
					increaseBalance(deserializedCaller);
					this.response = new MethodCallTransactionExceptionResponse((Exception) exception.getCause(), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					return;
				}

				if (exception != null)
					throw exception;

				if (isViewMethod && !onlyAffectedBalanceOf())
					throw new SideEffectsInViewMethodException(method);

				if (isVoidMethod) {
					MethodCallTransactionResponse response = new VoidMethodCallTransactionSuccessfulResponse(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					chargeForStorage(sizeCalculator.sizeOf(response));
					increaseBalance(deserializedCaller);
					this.response = new VoidMethodCallTransactionSuccessfulResponse(updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
				else {
					MethodCallTransactionResponse response = new MethodCallTransactionSuccessfulResponse
						(serializer.serialize(result), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					chargeForStorage(sizeCalculator.sizeOf(response));
					increaseBalance(deserializedCaller);
					this.response = new MethodCallTransactionSuccessfulResponse
						(serializer.serialize(result), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
			}
			catch (IllegalTransactionRequestException e) {
				throw e;
			}
			catch (Throwable t) {
				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
				BigInteger gasConsumedForPenalty = request.gas.subtract(gasConsumedForCPU()).subtract(gasConsumedForRAM()).subtract(gasConsumedForStorage());
				this.response = new MethodCallTransactionFailedResponse(wrapAsTransactionException(t, "Failed transaction"), balanceUpdateInCaseOfFailure, gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty);
			}
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "cannot complete the transaction");
		}
	}

	@Override
	public EngineClassLoaderImpl getClassLoader() {
		return classLoader;
	}

	@Override
	public MethodCallTransactionResponse getResponse() {
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

	/**
	 * Checks if the given object is a red/green externally owned account or subclass.
	 * 
	 * @param object the object to check
	 * @throws IllegalTransactionRequestException if the object is not a red/green externally owned account
	 */
	private void checkIsRedGreenExternallyOwned(Object object) throws ClassNotFoundException, IllegalTransactionRequestException {
		if (!classLoader.getRedGreenExternallyOwnedAccount().isAssignableFrom(object.getClass()))
			throw new IllegalTransactionRequestException("Only a red/green externally owned contract can start a transaction for a @RedPayable method or constructor");
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