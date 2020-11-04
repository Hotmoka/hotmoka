package io.takamaka.code.verification.issues;

public class IllegalFromContractArgumentError extends Error {

	public IllegalFromContractArgumentError(String where, String methodName) {
		super(where, methodName, -1, "the argument of @FromContract must be a contract");
	}
}