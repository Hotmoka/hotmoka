package io.takamaka.code.verification.issues;

public class CallerNotOnThisError extends Error {

	public CallerNotOnThisError(String where, String methodName, int line) {
		super(where, methodName, line, "caller() can only be called on \"this\"");
	}
}