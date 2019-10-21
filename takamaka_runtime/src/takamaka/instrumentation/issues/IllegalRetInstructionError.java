package takamaka.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class IllegalRetInstructionError extends Error {

	public IllegalRetInstructionError(ClassGen clazz, String methodName, int line) {
		super(clazz, methodName, line, "bytecode RET is not allowed");
	}
}