package io.takamaka.code.verification.issues;

public class IllegalModificationOfAmountInConstructorChaining extends Error {

	public IllegalModificationOfAmountInConstructorChaining(String where, String methodName, int line) {
		super(where, methodName, line, "the paid amount cannot be changed in constructor chaining");
	}
}