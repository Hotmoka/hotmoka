package io.takamaka.code.instrumentation.issues;

public class IllegalStaticInitializationError extends Error {

	public IllegalStaticInitializationError(String where, String methodName, int line) {
		super(where, methodName, line, "illegal static initialization: only primitive or string final static fields bound to constants are allowed");
	}
}