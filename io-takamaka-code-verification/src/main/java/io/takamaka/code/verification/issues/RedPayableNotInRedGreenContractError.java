package io.takamaka.code.verification.issues;

public class RedPayableNotInRedGreenContractError extends Error {

	public RedPayableNotInRedGreenContractError(String where, String methodName) {
		super(where, methodName, -1, "@RedPayable can only be used in red/green contracts");
	}
}