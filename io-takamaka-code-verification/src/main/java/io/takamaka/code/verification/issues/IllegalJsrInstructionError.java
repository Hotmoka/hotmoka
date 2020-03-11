package io.takamaka.code.verification.issues;

public class IllegalJsrInstructionError extends Error {

	public IllegalJsrInstructionError(String where, String methodName, int line) {
		super(where, methodName, line, "bytecode JSR is not allowed");
	}
}