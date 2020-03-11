package io.takamaka.code.verification.issues;

public class IllegalRetInstructionError extends Error {

	public IllegalRetInstructionError(String where, String methodName, int line) {
		super(where, methodName, line, "bytecode RET is not allowed");
	}
}