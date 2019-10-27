package io.takamaka.code.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class IllegalBootstrapMethodError extends Error {

	public IllegalBootstrapMethodError(ClassGen clazz) {
		super(clazz, "Illegal bootstrap method");
	}
}