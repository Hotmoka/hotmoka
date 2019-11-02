package io.takamaka.code.instrumentation.issues;

public class IllegalUpdateOfLocal0Error extends Error {

	public IllegalUpdateOfLocal0Error(String where, String methodName, int line) {
		super(where, methodName, line, "local 0 (\"this\") cannot be modified");
	}
}