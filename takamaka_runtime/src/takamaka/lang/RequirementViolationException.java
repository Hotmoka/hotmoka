package takamaka.lang;

/**
 * An exception thrown when a contract violates a requirement statement.
 */
public class RequirementViolationException extends IllegalStateException {
	public RequirementViolationException(String message) {
		super(message);
	}

	public RequirementViolationException(String message, Throwable cause) {
		super(message, cause);
	}
}