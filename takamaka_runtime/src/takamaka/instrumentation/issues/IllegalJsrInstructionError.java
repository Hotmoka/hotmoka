package takamaka.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class IllegalJsrInstructionError extends Error {

	public IllegalJsrInstructionError(ClassGen clazz, String methodName, int line) {
		super(clazz, methodName, line, "bytecode JSR is not allowed");
	}
}