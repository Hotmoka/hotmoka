package io.takamaka.code.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class IllegalTypeForStorageFieldError extends Error {

	public IllegalTypeForStorageFieldError(ClassGen clazz, String fieldName, boolean fieldIsEnum) {
		super(clazz, fieldName, "type not allowed for a field of a storage class"
			+ (fieldIsEnum ? ": it is an enumeration with instance non-transient fields" : ""));
	}
}