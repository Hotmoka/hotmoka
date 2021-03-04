package io.takamaka.code.verification.issues;

public class RedPayableNotInRedGreenContractError extends Error {

	public RedPayableNotInRedGreenContractError(String where, String methodName) {
		super(where, methodName, -1, "@RedPayable can only be applied to constructors or instance methods of a red/green contract class or of an interface");
	}
}