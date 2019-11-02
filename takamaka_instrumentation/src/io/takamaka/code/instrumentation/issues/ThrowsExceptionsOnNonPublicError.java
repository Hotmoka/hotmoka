package io.takamaka.code.instrumentation.issues;

public class ThrowsExceptionsOnNonPublicError extends Error {

	public ThrowsExceptionsOnNonPublicError(String where, String methodName) {
		super(where, methodName, -1, "@ThrowsExceptions can only be applied to a public constructor or method");
	}
}