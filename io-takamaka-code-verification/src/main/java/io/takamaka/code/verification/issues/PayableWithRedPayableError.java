package io.takamaka.code.verification.issues;

public class PayableWithRedPayableError extends Error {

	public PayableWithRedPayableError(String where, String methodName) {
		super(where, methodName, -1, "a @Payable method cannot be at same time declared as @RedPayable");
	}
}