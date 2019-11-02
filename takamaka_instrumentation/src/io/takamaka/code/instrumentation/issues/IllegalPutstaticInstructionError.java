package io.takamaka.code.instrumentation.issues;

public class IllegalPutstaticInstructionError extends Error {

	public IllegalPutstaticInstructionError(String where, String methodName, int line) {
		super(where, methodName, line, "static fields cannot be updated");
	}
}