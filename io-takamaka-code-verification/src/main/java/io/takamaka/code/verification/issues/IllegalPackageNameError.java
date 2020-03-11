package io.takamaka.code.verification.issues;

public class IllegalPackageNameError extends Error {

	public IllegalPackageNameError(String where) {
		super(where, "package name is not allowed");
	}
}