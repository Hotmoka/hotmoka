package io.takamaka.code.verification.issues;

public class RedPayableWithoutEntryError extends Error {

	public RedPayableWithoutEntryError(String where, String methodName) {
		super(where, methodName, -1, "@RedPayable can only be applied to an @Entry method or constructor");
	}
}