package io.takamaka.code.instrumentation.issues;

public class UncheckedExceptionHandlerError extends Error {

	public UncheckedExceptionHandlerError(String where, String methodName, int line, String exceptionName) {
		super(where, methodName, line, "exception handler for unchecked exception " + exceptionName);
	}
}