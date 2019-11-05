package io.takamaka.code.verification.issues;

public class PayableWithoutAmountError extends Error {

	public PayableWithoutAmountError(String where, String methodName) {
		super(where, methodName, -1, "a @Payable method must have a first argument for the payed amount, of type int, long or BigInteger");
	}
}