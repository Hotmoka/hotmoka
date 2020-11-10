package io.takamaka.code.verification.issues;

public class RedPayableWithoutFromContractError extends Error {

	public RedPayableWithoutFromContractError(String where, String methodName) {
		super(where, methodName, -1, "@RedPayable can only be applied to a @FromContract method or constructor");
	}
}