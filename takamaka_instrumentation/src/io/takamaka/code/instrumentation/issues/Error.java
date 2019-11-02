package io.takamaka.code.instrumentation.issues;

/**
 * A blocking issue: if an error occurs during the processing of a Takamaka jar file,
 * then its instrumentation cannot proceed and will be aborted.
 */
public abstract class Error extends Issue {

	protected Error(String where, String message) {
		super(where, message);
	}

	protected Error(String where, String fieldName, String message) {
		super(where, fieldName, message);
	}

	protected Error(String where, String methodName, int line, String message) {
		super(where, methodName, line, message);
	}
}