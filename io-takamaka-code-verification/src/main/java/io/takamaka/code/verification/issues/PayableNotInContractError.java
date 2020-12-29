package io.takamaka.code.verification.issues;

public class PayableNotInContractError extends Error {

	public PayableNotInContractError(String where, String methodName) {
		super(where, methodName, -1, "@Payable can only be used in contracts");
	}
}