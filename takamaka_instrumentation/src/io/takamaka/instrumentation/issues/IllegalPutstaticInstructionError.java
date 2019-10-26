package io.takamaka.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class IllegalPutstaticInstructionError extends Error {

	public IllegalPutstaticInstructionError(ClassGen clazz, String methodName, int line) {
		super(clazz, methodName, line, "static fields cannot be updated");
	}
}