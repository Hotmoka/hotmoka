package io.takamaka.code.verification.issues;

public class IllegalFinalizerError extends Error {

	public IllegalFinalizerError(String where, String methodName) {
		super(where, methodName, -1, "finalizers are not allowed");
	}
}