package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalRetInstructionError extends Error {

	public IllegalRetInstructionError(ClassGen clazz, Method where, int line) {
		super(clazz, where, line, "bytecode RET is not allowed");
	}
}