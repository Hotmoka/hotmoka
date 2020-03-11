package io.takamaka.code.verification.issues;

public class IllegalMethodNameError extends Error {

	public IllegalMethodNameError(String where, String fieldName) {
		super(where, fieldName, "method name \"" + fieldName + "\" is not allowed");
	}
}