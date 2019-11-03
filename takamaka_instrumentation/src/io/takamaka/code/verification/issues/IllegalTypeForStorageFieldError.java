package io.takamaka.code.verification.issues;

public class IllegalTypeForStorageFieldError extends Error {

	public IllegalTypeForStorageFieldError(String where, String fieldName, boolean fieldIsEnum) {
		super(where, fieldName, "type not allowed for a field of a storage class"
			+ (fieldIsEnum ? ": it is an enumeration with instance non-transient fields" : ""));
	}
}