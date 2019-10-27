package io.takamaka.code.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class IllegalUpdateOfLocal0Error extends Error {

	public IllegalUpdateOfLocal0Error(ClassGen clazz, String methodName, int line) {
		super(clazz, methodName, line, "local 0 (\"this\") cannot be modified");
	}
}