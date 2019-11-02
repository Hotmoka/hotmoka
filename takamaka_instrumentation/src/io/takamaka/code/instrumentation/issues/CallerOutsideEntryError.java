package io.takamaka.code.instrumentation.issues;

public class CallerOutsideEntryError extends Error {

	public CallerOutsideEntryError(String where, String methodName, int line) {
		super(where, methodName, line, "caller() can only be used inside an @Entry method or constructor");
	}
}