package io.takamaka.code.verification.issues;

public class IllegalFieldNameError extends Error {

	public IllegalFieldNameError(String where, String fieldName) {
		super(where, fieldName, "field name \"" + fieldName + "\" is not allowed");
	}
}