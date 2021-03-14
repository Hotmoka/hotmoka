package io.takamaka.code.verification.issues;

public class RedPayableNotInContractError extends Error {

	public RedPayableNotInContractError(String where, String methodName) {
		super(where, methodName, -1, "@RedPayable can only be applied to constructors or instance methods of a contract class or of an interface");
	}
}