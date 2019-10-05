package takamaka.verifier.errors;

import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class UncheckedExceptionHandlerError extends Error {

	public UncheckedExceptionHandlerError(ClassGen clazz, String methodName, int line, String exceptionName) {
		super(clazz, methodName, line, "exception handler for unchecked exception " + exceptionName);
	}
}