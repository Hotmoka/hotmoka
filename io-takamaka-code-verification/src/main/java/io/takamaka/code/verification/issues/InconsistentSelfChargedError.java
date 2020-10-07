package io.takamaka.code.verification.issues;

public class InconsistentSelfChargedError extends Error {

	public InconsistentSelfChargedError(String where, String methodName) {
		super(where, methodName, -1, "the @SelfCharged annotation can only be applied to public instance methods of contracts");
	}
}