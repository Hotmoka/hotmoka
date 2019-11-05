package io.takamaka.code.verification.issues;

public class IllegalBootstrapMethodError extends Error {

	public IllegalBootstrapMethodError(String where) {
		super(where, "Illegal bootstrap method");
	}
}