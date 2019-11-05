package io.takamaka.code.verification.issues;

public class InconsistentPayableError extends Error {

	public InconsistentPayableError(String where, String methodName, String clazzWhereItWasDefined) {
		super(where, methodName, -1, "@Payable is inconsistent with definition of the same method in class " + clazzWhereItWasDefined);
	}
}