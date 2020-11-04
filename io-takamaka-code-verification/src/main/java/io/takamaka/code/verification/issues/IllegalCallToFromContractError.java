package io.takamaka.code.verification.issues;

public class IllegalCallToFromContractError extends Error {

	public IllegalCallToFromContractError(String where, String methodName, String entryName, int line) {
		super(where, methodName, line, "\"" + entryName + "\" is @FromContract, hence can only be called from an instance method or constructor of a contract");
	}
}