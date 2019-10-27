package io.takamaka.code.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class IllegalSynchronizationError extends Error {

	public IllegalSynchronizationError(ClassGen clazz, String methodName) {
		super(clazz, methodName, -1, "synchronized methods are not allowed");
	}

	public IllegalSynchronizationError(ClassGen clazz, String methodName, int line) {
		super(clazz, methodName, line, "synchronization is not allowed");
	}
}