package takamaka.lang;

/**
 * An exception thrown when a contract violates an assertion statement.
 */
public class AssertionViolationException extends IllegalStateException {
	public AssertionViolationException(String message) {
		super(message);
	}

	public AssertionViolationException(String message, Throwable cause) {
		super(message, cause);
	}
}