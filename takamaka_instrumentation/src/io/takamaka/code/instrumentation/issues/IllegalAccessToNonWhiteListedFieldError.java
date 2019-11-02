package io.takamaka.code.instrumentation.issues;

public class IllegalAccessToNonWhiteListedFieldError extends Error {

	public IllegalAccessToNonWhiteListedFieldError(String where, String methodName, int line, String definingClassName, String fieldName) {
		super(where, methodName, line, "illegal access to non-white-listed field " + definingClassName + "." + fieldName);
	}
}