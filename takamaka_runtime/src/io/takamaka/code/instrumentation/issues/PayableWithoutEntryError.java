package io.takamaka.code.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class PayableWithoutEntryError extends Error {

	public PayableWithoutEntryError(ClassGen clazz, String methodName) {
		super(clazz, methodName, -1, "@Payable can only be applied to an @Entry method or constructor");
	}
}