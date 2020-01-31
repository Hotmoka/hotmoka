package io.takamaka.code.engine.internal.executors;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.values.StorageValue;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.internal.transactions.AbstractTransactionRun;
import io.takamaka.code.engine.internal.transactions.CodeCallTransactionRun;
import io.takamaka.code.engine.internal.transactions.NonInitialTransactionRun;

/**
 * The thread that executes a constructor of a storage object. It creates the class loader
 * from the class path and deserializes the actuals. Then calls the code and serializes
 * the resulting value back.
 */
public class ConstructorExecutor extends CodeExecutor<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> {

	/**
	 * Builds the executor of a constructor.
	 * 
	 * @param run the engine for which the constructor is being executed
	 * @param constructor the constructor to call
	 * @param deseralizedCaller the deserialized caller
	 * @param actuals the actuals provided to the constructor
	 * @throws TransactionException 
	 * @throws IllegalTransactionRequestException 
	 */
	public ConstructorExecutor(NonInitialTransactionRun<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> run, Stream<StorageValue> actuals) throws TransactionException, IllegalTransactionRequestException {
		super(run, null, actuals);
	}

	@Override
	public void run() {
		try {
			Constructor<?> constructorJVM;
			Object[] deserializedActuals;

			try {
				// we first try to call the constructor with exactly the parameter types explicitly provided
				constructorJVM = run.getConstructor(this);
				deserializedActuals = this.deserializedActuals;
			}
			catch (NoSuchMethodException e) {
				// if not found, we try to add the trailing types that characterize the @Entry constructors
				try {
					constructorJVM = run.getEntryConstructor(this);
					deserializedActuals = run.addExtraActualsForEntry(this);
				}
				catch (NoSuchMethodException ee) {
					throw e; // the message must be relative to the constructor as the user sees it
				}
			}

			run.ensureWhiteListingOf(this, constructorJVM, deserializedActuals);
			if (AbstractTransactionRun.hasAnnotation(constructorJVM, io.takamaka.code.constants.Constants.RED_PAYABLE_NAME))
				run.checkIsExternallyOwned(deserializedCaller);

			try {
				((CodeCallTransactionRun<?,?>)run).result = constructorJVM.newInstance(deserializedActuals);
			}
			catch (InvocationTargetException e) {
				((CodeCallTransactionRun<?,?>) run).exception = AbstractTransactionRun.unwrapInvocationException(e, constructorJVM);
			}
		}
		catch (Throwable t) {
			((CodeCallTransactionRun<?,?>) run).exception = t;
		}
	}
}