package io.takamaka.code.verification.issues;

public class IllegalCallToFromContractOnThisError extends Error {

	public IllegalCallToFromContractOnThisError(String where, String methodName, String entryName, int line) {
		super(where, methodName, line, "\"" + entryName + "\" is @FromContract and called on \"this\", hence can only be called from a @FromContract method or constructor");
	}
}