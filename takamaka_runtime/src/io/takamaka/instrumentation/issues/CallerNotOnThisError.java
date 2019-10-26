package io.takamaka.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class CallerNotOnThisError extends Error {

	public CallerNotOnThisError(ClassGen clazz, String methodName, int line) {
		super(clazz, methodName, line, "caller() can only be called on \"this\"");
	}
}