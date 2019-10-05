package takamaka.verifier.errors;

import org.apache.bcel.generic.ClassGen;

import takamaka.verifier.Error;

public class IllegalPutstaticInstructionError extends Error {

	public IllegalPutstaticInstructionError(ClassGen clazz, String methodName, int line) {
		super(clazz, methodName, line, "static fields cannot be updated");
	}
}