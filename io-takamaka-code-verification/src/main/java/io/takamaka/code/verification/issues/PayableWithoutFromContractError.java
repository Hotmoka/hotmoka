package io.takamaka.code.verification.issues;

public class PayableWithoutFromContractError extends Error {

	public PayableWithoutFromContractError(String where, String methodName) {
		super(where, methodName, -1, "@Payable can only be applied to a @FromContract method or constructor");
	}
}