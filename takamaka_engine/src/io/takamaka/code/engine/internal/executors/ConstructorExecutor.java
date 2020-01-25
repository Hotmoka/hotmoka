package io.takamaka.code.engine.internal.executors;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.values.StorageValue;
import io.takamaka.code.engine.TransactionRun;

/**
 * The thread that executes a constructor of a storage object. It creates the class loader
 * from the class path and deserializes the actuals. Then calls the code and serializes
 * the resulting value back.
 */
public class ConstructorExecutor extends CodeExecutor {

	/**
	 * Builds the executor of a constructor.
	 * 
	 * @param run the engine for which the constructor is being executed
	 * @param constructor the constructor to call
	 * @param deseralizedCaller the deserialized caller
	 * @param actuals the actuals provided to the constructor
	 */
	public ConstructorExecutor(TransactionRun run, ConstructorSignature constructor, Object deserializedCaller, Stream<StorageValue> actuals) {
		super(run, deserializedCaller, constructor, null, actuals);
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

		return run.getClassLoader().resolveConstructor(methodOrConstructor.definingClass.name, argTypes)
				.orElseThrow(() -> new NoSuchMethodException(methodOrConstructor.toString()));
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

		return run.getClassLoader().resolveConstructor(methodOrConstructor.definingClass.name, argTypes)
				.orElseThrow(() -> new NoSuchMethodException(methodOrConstructor.toString()));
	}

	@Override
	public void run() {
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
			if (hasAnnotation(constructorJVM, io.takamaka.code.constants.Constants.RED_PAYABLE_NAME))
				checkIsRedGreenExternallyOwned(deserializedCaller);

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
}