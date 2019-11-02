package io.takamaka.code.instrumentation.issues;

public class IllegalPackageNameError extends Error {

	public IllegalPackageNameError(String where) {
		super(where, "package name is not allowed");
	}
}