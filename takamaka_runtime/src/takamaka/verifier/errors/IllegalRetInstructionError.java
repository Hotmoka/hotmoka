package takamaka.verifier.errors;

import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalRetInstructionError extends Error {

	public IllegalRetInstructionError(ClassGen clazz, String methodName, int line) {
		super(clazz, methodName, line, "bytecode RET is not allowed");
	}
}