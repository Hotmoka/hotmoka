package io.takamaka.code.verification.issues;

public class PayableWithoutEntryError extends Error {

	public PayableWithoutEntryError(String where, String methodName) {
		super(where, methodName, -1, "@Payable can only be applied to an @Entry method or constructor");
	}
}