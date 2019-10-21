package takamaka.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class UncheckedExceptionHandlerError extends Error {

	public UncheckedExceptionHandlerError(ClassGen clazz, String methodName, int line, String exceptionName) {
		super(clazz, methodName, line, "exception handler for unchecked exception " + exceptionName);
	}
}