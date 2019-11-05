package io.takamaka.code.verification.issues;

public class IllegalSynchronizationError extends Error {

	public IllegalSynchronizationError(String where, String methodName) {
		super(where, methodName, -1, "synchronized methods are not allowed");
	}

	public IllegalSynchronizationError(String where, String methodName, int line) {
		super(where, methodName, line, "synchronization is not allowed");
	}
}