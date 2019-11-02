package io.takamaka.code.instrumentation.issues;

public class IllegalEntryMethodError extends Error {

	public IllegalEntryMethodError(String where, String methodName) {
		super(where, methodName, -1, "@Entry can only be applied to constructors or instance methods of a contract");
	}
}