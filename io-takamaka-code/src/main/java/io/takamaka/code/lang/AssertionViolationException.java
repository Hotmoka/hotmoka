package io.takamaka.code.lang;

/**
 * An exception thrown when a contract violates an assertion statement.
 */
@SuppressWarnings("serial")
public class AssertionViolationException extends RuntimeException {
	public AssertionViolationException(String message) {
		super(message);
	}

	public AssertionViolationException(String message, Throwable cause) {
		super(message, cause);
	}
}