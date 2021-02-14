package io.takamaka.code.verification.issues;

public class IllegalCallToRedPayableConstructorOnThis extends Error {

	public IllegalCallToRedPayableConstructorOnThis(String where, String methodName, String entryName, int line) {
		super(where, methodName, line, "only a @RedPayable constructor can call another @RedPayable constructor by chaining");
	}
}