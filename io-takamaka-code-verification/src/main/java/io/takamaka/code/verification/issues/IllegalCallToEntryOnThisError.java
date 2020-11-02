package io.takamaka.code.verification.issues;

public class IllegalCallToEntryOnThisError extends Error {

	public IllegalCallToEntryOnThisError(String where, String methodName, String entryName, int line) {
		super(where, methodName, line, "\"" + entryName + "\" is an @Entry called on \"this\", hence can only be called from an @Entry method or constructor");
	}
}