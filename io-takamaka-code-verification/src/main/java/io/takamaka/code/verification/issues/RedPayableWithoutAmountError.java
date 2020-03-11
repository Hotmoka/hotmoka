package io.takamaka.code.verification.issues;

public class RedPayableWithoutAmountError extends Error {

	public RedPayableWithoutAmountError(String where, String methodName) {
		super(where, methodName, -1, "a @RedPayable method must have a first argument for the payed amount, of type int, long or BigInteger");
	}
}