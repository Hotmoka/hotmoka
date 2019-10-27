package io.takamaka.code.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class IllegalAccessToNonWhiteListedFieldError extends Error {

	public IllegalAccessToNonWhiteListedFieldError(ClassGen clazz, String methodName, int line, String definingClassName, String fieldName) {
		super(clazz, methodName, line, "illegal access to non-white-listed field " + definingClassName + "." + fieldName);
	}
}