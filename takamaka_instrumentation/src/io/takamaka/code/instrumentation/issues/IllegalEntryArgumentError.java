package io.takamaka.code.instrumentation.issues;

public class IllegalEntryArgumentError extends Error {

	public IllegalEntryArgumentError(String where, String methodName) {
		super(where, methodName, -1, "@Entry argument is not a contract");
	}
}