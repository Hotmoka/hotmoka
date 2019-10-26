package io.takamaka.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class IllegalStaticInitializationError extends Error {

	public IllegalStaticInitializationError(ClassGen clazz, String methodName, int line) {
		super(clazz, methodName, line, "illegal static initialization: only primitive or string final static fields bound to constants are allowed");
	}
}