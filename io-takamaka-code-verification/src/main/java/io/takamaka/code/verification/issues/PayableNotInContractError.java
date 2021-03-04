package io.takamaka.code.verification.issues;

public class PayableNotInContractError extends Error {

	public PayableNotInContractError(String where, String methodName) {
		super(where, methodName, -1, "@Payable can only be applied to constructors or instance methods of a contract class or of an interface");
	}
}