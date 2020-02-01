package io.takamaka.code.engine.internal.transactions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Optional;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.NonWhiteListedCallException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;

public class ConstructorCallTransactionRun extends CodeCallTransactionRun<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> {
	private final CodeSignature constructor;
	private final EngineClassLoaderImpl classLoader;

	/**
	 * The response computed at the end of the transaction.
	 */
	private ConstructorCallTransactionResponse response; // TODO: make final

	public ConstructorCallTransactionRun(ConstructorCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException, IllegalTransactionRequestException {
		super(request, current, node);

		this.constructor = request.constructor;

		try (EngineClassLoaderImpl classLoader = new EngineClassLoaderImpl(request.classpath, this)) {
			this.classLoader = classLoader;
			
			try {
				this.deserializedCaller = deserializer.deserialize(request.caller);
				this.deserializedActuals = request.actuals().map(deserializer::deserialize).toArray(Object[]::new);
				checkIsExternallyOwned(deserializedCaller);
				// we sell all gas first: what remains will be paid back at the end;
				// if the caller has not enough to pay for the whole gas, the transaction won't be executed
				this.balanceUpdateInCaseOfFailure = checkMinimalGas(request, deserializedCaller);
				chargeForCPU(node.getGasCostModel().cpuBaseTransactionCost());
				chargeForStorage(sizeCalculator.sizeOf(request));
			}
			catch (IllegalTransactionRequestException e) {
				throw e;
			}
			catch (Throwable t) {
				throw wrapAsTransactionException(t, "cannot complete the transaction");
			}

			try {
				Thread executor = new Thread(this::run);
				executor.start();
				executor.join();

				if (exception instanceof InvocationTargetException) {
					ConstructorCallTransactionResponse response = new ConstructorCallTransactionExceptionResponse((Exception) exception.getCause(), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					chargeForStorage(sizeCalculator.sizeOf(response));
					increaseBalance(deserializedCaller);
					this.response = new ConstructorCallTransactionExceptionResponse((Exception) exception.getCause(), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					return;
				}

				if (exception != null)
					throw exception;

				ConstructorCallTransactionResponse response = new ConstructorCallTransactionSuccessfulResponse
					((StorageReference) serializer.serialize(result), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				chargeForStorage(sizeCalculator.sizeOf(response));
				increaseBalance(deserializedCaller);
				this.response = new ConstructorCallTransactionSuccessfulResponse
					((StorageReference) serializer.serialize(result), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
			catch (IllegalTransactionRequestException e) {
				throw e;
			}
			catch (Throwable t) {
				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
				BigInteger gasConsumedForPenalty = request.gas.subtract(gasConsumedForCPU()).subtract(gasConsumedForRAM()).subtract(gasConsumedForStorage());
				this.response = new ConstructorCallTransactionFailedResponse(wrapAsTransactionException(t, "failed transaction"), balanceUpdateInCaseOfFailure, gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty);
			}
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "cannot complete the transaction");
		}
	}

	private void run() {
		try {
			Constructor<?> constructorJVM;
			Object[] deserializedActuals;

			try {
				// we first try to call the constructor with exactly the parameter types explicitly provided
				constructorJVM = getConstructor();
				deserializedActuals = this.deserializedActuals;
			}
			catch (NoSuchMethodException e) {
				// if not found, we try to add the trailing types that characterize the @Entry constructors
				try {
					constructorJVM = getEntryConstructor();
					deserializedActuals = addExtraActualsForEntry();
				}
				catch (NoSuchMethodException ee) {
					throw e; // the message must be relative to the constructor as the user sees it
				}
			}

			ensureWhiteListingOf(constructorJVM, deserializedActuals);
			if (hasAnnotation(constructorJVM, Constants.RED_PAYABLE_NAME))
				checkIsExternallyOwned(deserializedCaller);

			try {
				result = constructorJVM.newInstance(deserializedActuals);
			}
			catch (InvocationTargetException e) {
				exception = unwrapInvocationException(e, constructorJVM);
			}
		}
		catch (Throwable t) {
			exception = t;
		}
	}

	@Override
	public EngineClassLoaderImpl getClassLoader() {
		return classLoader;
	}

	@Override
	public ConstructorCallTransactionResponse getResponse() {
		return response;
	}

	/**
	 * Checks that the constructor called by this transaction is
	 * white-listed and its white-listing proof-obligations hold.
	 * 
	 * @param executable the constructor
	 * @param actuals the actual arguments passed to {@code executable}
	 * @throws ClassNotFoundException if some class could not be found during the check
	 */
	private void ensureWhiteListingOf(Constructor<?> executable, Object[] actuals) throws ClassNotFoundException {
		Optional<Constructor<?>> model = classLoader.getWhiteListingWizard().whiteListingModelOf((Constructor<?>) executable);
		if (!model.isPresent())
			throw new NonWhiteListedCallException("illegal call to non-white-listed constructor of " + constructor.definingClass.name);

		Annotation[][] anns = model.get().getParameterAnnotations();
		for (int pos = 0; pos < anns.length; pos++)
			checkWhiteListingProofObligations(model.get().getName(), actuals[pos], anns[pos]);
	}

	/**
	 * Resolves the constructor that must be called.
	 * 
	 * @return the constructor
	 * @throws NoSuchMethodException if the constructor could not be found
	 * @throws SecurityException if the constructor could not be accessed
	 * @throws ClassNotFoundException if the class of the constructor or of some parameter cannot be found
	 */
	private Constructor<?> getConstructor() throws ClassNotFoundException, NoSuchMethodException {
		Class<?>[] argTypes = formalsAsClass();

		return classLoader.resolveConstructor(constructor.definingClass.name, argTypes)
			.orElseThrow(() -> new NoSuchMethodException(constructor.toString()));
	}

	/**
	 * Resolves the constructor that must be called, assuming that it is an entry.
	 * 
	 * @return the constructor
	 * @throws NoSuchMethodException if the constructor could not be found
	 * @throws SecurityException if the constructor could not be accessed
	 * @throws ClassNotFoundException if the class of the constructor or of some parameter cannot be found
	 */
	private Constructor<?> getEntryConstructor() throws ClassNotFoundException, NoSuchMethodException {
		Class<?>[] argTypes = formalsAsClassForEntry();

		return classLoader.resolveConstructor(constructor.definingClass.name, argTypes)
			.orElseThrow(() -> new NoSuchMethodException(constructor.toString()));
	}

	@Override
	protected final CodeSignature getMethodOrConstructor() {
		return constructor;
	}
}