package io.takamaka.code.instrumentation.issues;

public class IllegalRetInstructionError extends Error {

	public IllegalRetInstructionError(String where, String methodName, int line) {
		super(where, methodName, line, "bytecode RET is not allowed");
	}
}