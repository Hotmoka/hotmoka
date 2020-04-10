package io.takamaka.code.engine.internal.transactions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.NonWhiteListedCallException;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.internal.EngineClassLoader;

/**
 * The creator of a transaction that executes a constructor of Takamaka code.
 */
public class ConstructorCallTransactionBuilder extends CodeCallTransactionBuilder<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> {

	/**
	 * The class loader used for the transaction.
	 */
	private final EngineClassLoader classLoader;

	/**
	 * The constructor that is being called.
	 */
	private final CodeSignature constructor;

	/**
	 * The deserialized caller.
	 */
	private final Object deserializedCaller;

	/**
	 * The deserialized actual arguments of the constructor.
	 */
	private final Object[] deserializedActuals;

	/**
	 * The response computed at the end of the transaction.
	 */
	private final ConstructorCallTransactionResponse response;

	/**
	 * Builds the creator of a transaction that executes a constructor of Takamaka code.
	 * 
	 * @param request the request of the transaction
	 * @param current the reference that must be used for the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public ConstructorCallTransactionBuilder(ConstructorCallTransactionRequest request, TransactionReference current, Node node) throws TransactionRejectedException {
		super(request, current, node);

		try {
			this.constructor = request.constructor;
			this.classLoader = new EngineClassLoader(request.classpath, this);

			if (request.constructor.formals().count() != request.actuals().count())
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

			ConstructorCallTransactionResponse response = null;
			Constructor<?> constructorJVM = null;
			try {
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
					callerMustBeARedGreenExternallyOwnedAccount();

				ConstructorThread thread = new ConstructorThread(constructorJVM, deserializedActuals);
				thread.start();
				thread.join();
				if (thread.exception != null)
					if (thread.exception instanceof InvocationTargetException) {
						Throwable cause = thread.exception.getCause();
						if (isCheckedForThrowsExceptions(cause, constructorJVM)) {
							chargeForStorage(new ConstructorCallTransactionExceptionResponse(cause.getClass().getName(), cause.getMessage(), where(cause), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
							payBackRemainingGas();
							increaseNonceOfCaller();
							response = new ConstructorCallTransactionExceptionResponse(cause.getClass().getName(), cause.getMessage(), where(cause), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());						
						}
						else
							throw cause;
					}
					else
						throw thread.exception;
				else {
					chargeForStorage(new ConstructorCallTransactionSuccessfulResponse
						((StorageReference) serializer.serialize(thread.result), updates(thread.result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
					payBackRemainingGas();
					increaseNonceOfCaller();
					response = new ConstructorCallTransactionSuccessfulResponse
						((StorageReference) serializer.serialize(thread.result), updates(thread.result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
				}
			}
			catch (Throwable t) {
				increaseNonceOfCaller();
				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
				response = new ConstructorCallTransactionFailedResponse(t.getClass().getName(), t.getMessage(), where(t), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
			}

			this.response = response;
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	public final EngineClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public final ConstructorCallTransactionResponse getResponse() {
		return response;
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

	@Override
	protected final Object getDeserializedCaller() {
		return deserializedCaller;
	}

	@Override
	protected final Stream<Object> getDeserializedActuals() {
		return Stream.of(deserializedActuals);
	}

	@Override
	protected final BigInteger gasForStoringFailedResponse() {
		BigInteger gas = gas();

		return sizeCalculator.sizeOfResponse(new ConstructorCallTransactionFailedResponse
			("placeholder for the name of the exception", "placeholder for the message of the exception", "placeholder for where",
			updatesToBalanceOrNonceOfCaller(), gas, gas, gas, gas));
	}

	/**
	 * The thread that deserializes the caller and the actual parameters.
	 * This must be done inside a thread so that static initializers
	 * are run with an associated {@code io.takamaka.code.engine.internal.Runtime} object.
	 */
	private class DeserializerThread extends TakamakaThread {
		private final ConstructorCallTransactionRequest request;

		/**
		 * The deserialized caller.
		 */
		private Object deserializedCaller;

		/**
		 * The deserialized actual arguments of the call.
		 */
		private Object[] deserializedActuals;

		private DeserializerThread(ConstructorCallTransactionRequest request) {
			this.request = request;
		}

		@Override
		protected void body() {
			this.deserializedCaller = deserializer.deserialize(request.caller);
			this.deserializedActuals = request.actuals().map(deserializer::deserialize).toArray(Object[]::new);
		}
	}

	/**
	 * The thread that runs the constructor.
	 */
	private class ConstructorThread extends TakamakaThread {
		private Object result;
		private final Constructor<?> constructorJVM;
		private final Object[] deserializedActuals;

		private ConstructorThread(Constructor<?> constructorJVM, Object[] deserializedActuals) {
			this.constructorJVM = constructorJVM;
			this.deserializedActuals = deserializedActuals;
		}

		@Override
		protected void body() throws Exception {
			result = constructorJVM.newInstance(deserializedActuals);
		}
	}
}