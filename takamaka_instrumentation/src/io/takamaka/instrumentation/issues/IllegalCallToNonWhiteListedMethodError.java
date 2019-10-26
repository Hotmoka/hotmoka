package io.takamaka.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class IllegalCallToNonWhiteListedMethodError extends Error {

	public IllegalCallToNonWhiteListedMethodError(ClassGen clazz, String methodName, int line, String declaringClassName, String calledMethodName) {
		super(clazz, methodName, line, "illegal call to non-white-listed method " + declaringClassName + "." + calledMethodName);
	}
}