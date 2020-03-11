package io.takamaka.code.verification.issues;

public class IllegalCallToNonWhiteListedConstructorError extends Error {

	public IllegalCallToNonWhiteListedConstructorError(String where, String methodName, int line, String declaringClassName) {
		super(where, methodName, line, "illegal call to non-white-listed constructor of " + declaringClassName);
	}
}