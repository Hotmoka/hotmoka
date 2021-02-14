package io.takamaka.code.verification.issues;

public class IllegalCallToPayableConstructorOnThis extends Error {

	public IllegalCallToPayableConstructorOnThis(String where, String methodName, String entryName, int line) {
		super(where, methodName, line, "only a @Payable constructor can call another @Payable constructor by chaining");
	}
}