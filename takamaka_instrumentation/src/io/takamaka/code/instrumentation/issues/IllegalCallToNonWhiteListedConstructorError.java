package io.takamaka.code.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class IllegalCallToNonWhiteListedConstructorError extends Error {

	public IllegalCallToNonWhiteListedConstructorError(ClassGen clazz, String methodName, int line, String declaringClassName) {
		super(clazz, methodName, line, "illegal call to non-white-listed constructor of " + declaringClassName);
	}
}