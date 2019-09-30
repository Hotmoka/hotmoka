package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalJsrInstructionError extends Error {

	public IllegalJsrInstructionError(ClassGen clazz, Method where, int line) {
		super(clazz, where, line, "bytecode JSR is not allowed");
	}
}