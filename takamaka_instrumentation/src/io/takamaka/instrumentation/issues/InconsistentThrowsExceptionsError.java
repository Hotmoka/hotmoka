package io.takamaka.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class InconsistentThrowsExceptionsError extends Error {

	public InconsistentThrowsExceptionsError(ClassGen clazz, String methodName, String clazzWhereItWasDefined) {
		super(clazz, methodName, -1, "@ThrowsExceptions is inconsistent with definition of the same method in class " + clazzWhereItWasDefined);
	}
}