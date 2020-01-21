package io.takamaka.code.engine.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageValue;
import io.takamaka.code.engine.AbstractBlockchain;

/**
 * The thread that executes a static method of a storage object. It creates the class loader
 * from the class path and deserializes the actuals. Then calls the code and serializes
 * the resulting value back.
 */
public class StaticMethodExecutor extends CodeExecutor {

	/**
	 * Builds the executor of a static method.
	 * 
	 * @param engine the engine for which the method is being executed
	 * @param method the method to call
	 * @param caller the caller, that pays for the execution
	 * @param deseralizedCaller the deserialized caller
	 * @param actuals the actuals provided to the method
	 */
	public StaticMethodExecutor(AbstractBlockchain engine, MethodSignature method, Object deserializedCaller, Stream<StorageValue> actuals) {
		super(engine, deserializedCaller, method, null, actuals);
	}

	@Override
	public void run() {
		try {
			Method methodJVM = getMethod();

			if (!Modifier.isStatic(methodJVM.getModifiers()))
				throw new NoSuchMethodException("Cannot call an instance method: use addInstanceMethodCallTransaction instead");

			ensureWhiteListingOf(methodJVM, deserializedActuals);

			isVoidMethod = methodJVM.getReturnType() == void.class;
			isViewMethod = hasAnnotation(methodJVM, io.takamaka.code.constants.Constants.VIEW_NAME);

			try {
				result = methodJVM.invoke(null, deserializedActuals);
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