package io.takamaka.code.verification.issues;

public class InconsistentThrowsExceptionsError extends Error {

	public InconsistentThrowsExceptionsError(String where, String methodName, String clazzWhereItWasDefined) {
		super(where, methodName, -1, "@ThrowsExceptions is inconsistent with definition of the same method in class " + clazzWhereItWasDefined);
	}
}