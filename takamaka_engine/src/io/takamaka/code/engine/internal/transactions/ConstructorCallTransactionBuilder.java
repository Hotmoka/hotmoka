package io.takamaka.code.engine.internal.transactions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.NonWhiteListedCallException;
import io.takamaka.code.engine.internal.EngineClassLoader;

public class ConstructorCallTransactionBuilder extends CodeCallTransactionBuilder<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> {
	private final CodeSignature constructor;
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
	private final ConstructorCallTransactionResponse response;

	public ConstructorCallTransactionBuilder(ConstructorCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node);

		this.constructor = request.constructor;

		try (EngineClassLoader classLoader = new EngineClassLoader(request.classpath, this)) {
			this.classLoader = classLoader;
			this.deserializedCaller = deserializer.deserialize(request.caller);
			this.deserializedActuals = request.actuals().map(deserializer::deserialize).toArray(Object[]::new);
			checkIsExternallyOwned(deserializedCaller);
			// we sell all gas first: what remains will be paid back at the end;
			// if the caller has not enough to pay for the whole gas, the transaction won't be executed
			UpdateOfBalance balanceUpdateInCaseOfFailure = checkMinimalGas(request, deserializedCaller);
			chargeForCPU(node.getGasCostModel().cpuBaseTransactionCost());
			chargeForStorage(request);

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
					checkIsRedGreenExternallyOwned(deserializedCaller);

				Object result = constructorJVM.newInstance(deserializedActuals);
				chargeForStorage(new ConstructorCallTransactionSuccessfulResponse
					((StorageReference) serializer.serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				increaseBalance(deserializedCaller);
				response = new ConstructorCallTransactionSuccessfulResponse
					((StorageReference) serializer.serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
			catch (InvocationTargetException e) {
				if (isCheckedForThrowsExceptions(e, constructorJVM)) {
					chargeForStorage(new ConstructorCallTransactionExceptionResponse((Exception) e.getCause(), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
					increaseBalance(deserializedCaller);
					response = new ConstructorCallTransactionExceptionResponse((Exception) e.getCause(), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());						
				}
				else
					throw e.getCause();
			}
			catch (Throwable t) {
				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
				response = new ConstructorCallTransactionFailedResponse(wrapAsTransactionException(t), balanceUpdateInCaseOfFailure, gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
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
}