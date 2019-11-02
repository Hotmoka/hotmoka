package io.takamaka.code.instrumentation.issues;

public class InconsistentEntryError extends Error {

	public InconsistentEntryError(String where, String methodName, String clazzWhereItWasDefined) {
		super(where, methodName, -1, "@Entry is inconsistent with definition of the same method in class " + clazzWhereItWasDefined);
	}
}