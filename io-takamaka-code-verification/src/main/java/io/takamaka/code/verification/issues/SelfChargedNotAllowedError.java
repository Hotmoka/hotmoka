package io.takamaka.code.verification.issues;

public class SelfChargedNotAllowedError extends Error {

	public SelfChargedNotAllowedError(String where, String methodName) {
		super(where, methodName, -1, "the @SelfCharged annotation is not allowed");
	}
}