package takamaka.verifier;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

public class IllegalJsrInstructionError extends Error {

	public IllegalJsrInstructionError(ClassGen clazz, Method where, int line) {
		super(clazz, where, line, "bytecode JSR is not allowed in Takamaka code");
	}
}