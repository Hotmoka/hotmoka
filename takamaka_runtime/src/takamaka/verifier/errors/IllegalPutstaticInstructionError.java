package takamaka.verifier.errors;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalPutstaticInstructionError extends Error {

	public IllegalPutstaticInstructionError(ClassGen clazz, Method where, int line) {
		super(clazz, where, line, "static fields cannot be updated");
	}
}