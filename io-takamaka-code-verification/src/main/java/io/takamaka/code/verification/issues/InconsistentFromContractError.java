package io.takamaka.code.verification.issues;

public class InconsistentFromContractError extends Error {

	public InconsistentFromContractError(String where, String methodName, String clazzWhereItWasDefined) {
		super(where, methodName, -1, "@FromContract is inconsistent with the definition of the same method in class " + clazzWhereItWasDefined);
	}
}