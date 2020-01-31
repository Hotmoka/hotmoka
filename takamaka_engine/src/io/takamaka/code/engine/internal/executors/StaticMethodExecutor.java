package io.takamaka.code.engine.internal.executors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.values.StorageValue;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.internal.transactions.AbstractTransactionRun;
import io.takamaka.code.engine.internal.transactions.CodeCallTransactionRun;
import io.takamaka.code.engine.internal.transactions.MethodCallTransactionRun;
import io.takamaka.code.engine.internal.transactions.NonInitialTransactionRun;

/**
 * The thread that executes a static method of a storage object. It creates the class loader
 * from the class path and deserializes the actuals. Then calls the code and serializes
 * the resulting value back.
 */
public class StaticMethodExecutor extends CodeExecutor<StaticMethodCallTransactionRequest, MethodCallTransactionResponse> {

	/**
	 * Builds the executor of a static method.
	 * 
	 * @param run the engine for which the method is being executed
	 * @param method the method to call
	 * @param caller the caller, that pays for the execution
	 * @param actuals the actuals provided to the method
	 * @throws TransactionException 
	 * @throws IllegalTransactionRequestException 
	 */
	public StaticMethodExecutor(NonInitialTransactionRun<StaticMethodCallTransactionRequest, MethodCallTransactionResponse> run, Stream<StorageValue> actuals) throws TransactionException, IllegalTransactionRequestException {
		super(run, null, actuals);
	}

	@Override
	public void run() {
		try {
			Method methodJVM = run.getMethod(this);

			if (!Modifier.isStatic(methodJVM.getModifiers()))
				throw new NoSuchMethodException("Cannot call an instance method: use addInstanceMethodCallTransaction instead");

			run.ensureWhiteListingOf(this, methodJVM, deserializedActuals);

			((MethodCallTransactionRun<?>) run).isVoidMethod = methodJVM.getReturnType() == void.class;
			((MethodCallTransactionRun<?>) run).isViewMethod = AbstractTransactionRun.hasAnnotation(methodJVM, io.takamaka.code.constants.Constants.VIEW_NAME);

			try {
				((CodeCallTransactionRun<?,?>) run).result = methodJVM.invoke(null, deserializedActuals);
			}
			catch (InvocationTargetException e) {
				((CodeCallTransactionRun<?,?>) run).exception = AbstractTransactionRun.unwrapInvocationException(e, methodJVM);
			}
		}
		catch (Throwable t) {
			((CodeCallTransactionRun<?,?>) run).exception = t;
		}
	}
}