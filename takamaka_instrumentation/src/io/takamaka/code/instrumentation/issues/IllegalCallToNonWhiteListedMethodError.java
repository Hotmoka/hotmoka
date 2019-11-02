package io.takamaka.code.instrumentation.issues;

public class IllegalCallToNonWhiteListedMethodError extends Error {

	public IllegalCallToNonWhiteListedMethodError(String where, String methodName, int line, String declaringClassName, String calledMethodName) {
		super(where, methodName, line, "illegal call to non-white-listed method " + declaringClassName + "." + calledMethodName);
	}
}