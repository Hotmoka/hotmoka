package io.takamaka.code.lang;

/**
 * An exception thrown when a contract violates a requirement statement.
 */
@SuppressWarnings("serial")
public class RequirementViolationException extends RuntimeException {
	public RequirementViolationException(String message) {
		super(message);
	}

	public RequirementViolationException(String message, Throwable cause) {
		super(message, cause);
	}
}