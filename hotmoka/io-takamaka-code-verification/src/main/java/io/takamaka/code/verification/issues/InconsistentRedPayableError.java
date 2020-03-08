package io.takamaka.code.verification.issues;

public class InconsistentRedPayableError extends Error {

	public InconsistentRedPayableError(String where, String methodName, String clazzWhereItWasDefined) {
		super(where, methodName, -1, "@RedPayable is inconsistent with definition of the same method in class " + clazzWhereItWasDefined);
	}
}