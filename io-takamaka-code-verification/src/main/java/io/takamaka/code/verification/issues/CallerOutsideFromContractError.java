package io.takamaka.code.verification.issues;

public class CallerOutsideFromContractError extends Error {

	public CallerOutsideFromContractError(String where, String methodName, int line) {
		super(where, methodName, line, "caller() can only be used inside a @FromContract method or constructor");
	}
}