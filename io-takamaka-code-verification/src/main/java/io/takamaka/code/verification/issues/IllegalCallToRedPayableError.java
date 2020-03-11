package io.takamaka.code.verification.issues;

public class IllegalCallToRedPayableError extends Error {

	public IllegalCallToRedPayableError(String where, String methodName, String calleeName, int line) {
		super(where, methodName, line, "\"" + calleeName + "\" is @RedPayable, hence can only be called from a red/green contract");
	}
}