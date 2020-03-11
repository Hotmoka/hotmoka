package io.takamaka.code.verification.issues;

/**
 * A non-blocking issue: if a warning occurs during the processing of a Takamaka jar file,
 * then its instrumentation does proceed and will not be aborted.
 */
public abstract class Warning extends Issue {

	protected Warning(String where, String message) {
		super(where, message);
	}

	protected Warning(String where, String methodName, int line, String message) {
		super(where, methodName, line, message);
	}

	protected Warning(String where, String fieldName, String message) {
		super(where, fieldName, message);
	}
}