package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class IllegalRetInstructionError extends Error {

	public IllegalRetInstructionError(ClassGen clazz, Method where, int line) {
		super(clazz, where, line, "bytecode RET is not allowed");
	}
}