package io.takamaka.code.engine.internal.executors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.takamaka.code.engine.internal.transactions.AbstractTransactionRun;

/**
 * The thread that executes an instance method of a storage object. It creates the class loader
 * from the class path and deserializes receiver and actuals. Then calls the code and serializes
 * the resulting value back.
 */
public class InstanceMethodExecutor extends CodeExecutor {

	/**
	 * Builds the executor of an instance method.
	 * 
	 * @param run the engine for which the method is being executed
	 * @param method the method to call
	 * @param deseralizedCaller the deserialized caller
	 * @param receiver the receiver of the method
	 * @param actuals the actuals provided to the method
	 */
	public InstanceMethodExecutor(AbstractTransactionRun<?,?> run, MethodSignature method, Object deserializedCaller, StorageReference receiver, Stream<StorageValue> actuals) {
		super(run, deserializedCaller, method, receiver, actuals);
	}

	@Override
	public void run() {
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
				throw new NoSuchMethodException("Cannot call a static method: use addStaticMethodCallTransaction instead");

			ensureWhiteListingOf(methodJVM, deserializedActuals);

			isVoidMethod = methodJVM.getReturnType() == void.class;
			isViewMethod = hasAnnotation(methodJVM, io.takamaka.code.constants.Constants.VIEW_NAME);
			if (hasAnnotation(methodJVM, io.takamaka.code.constants.Constants.RED_PAYABLE_NAME))
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
	}
}