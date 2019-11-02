package io.takamaka.code.instrumentation.issues;

public class IllegalCallToEntryError extends Error {

	public IllegalCallToEntryError(String where, String methodName, String entryName, int line) {
		super(where, methodName, line, "\"" + entryName + "\" is an @Entry, hence can only be called from an instance method of a contract");
	}
}