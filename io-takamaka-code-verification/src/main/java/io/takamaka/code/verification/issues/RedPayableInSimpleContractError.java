package io.takamaka.code.verification.issues;

public class RedPayableInSimpleContractError extends Error {

	public RedPayableInSimpleContractError(String where, String methodName) {
		super(where, methodName, -1, "a @RedPayable method or constructor can only be defined in a red/green contract");
	}
}