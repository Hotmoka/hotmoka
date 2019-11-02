package io.takamaka.code.instrumentation.issues;

public class IllegalBootstrapMethodError extends Error {

	public IllegalBootstrapMethodError(String where) {
		super(where, "Illegal bootstrap method");
	}
}