package io.hotmoka.beans;

/**
 * An unexpected error, during the execution of some Hotmoka code.
 */
public class InternalFailureException extends RuntimeException {
	private static final long serialVersionUID = 3975906281624182199L;

	private InternalFailureException(Throwable t) {
		super(t.getMessage(), t);
	}

	private InternalFailureException(String message, Throwable t) {
		super(message + ": " + t.getMessage(), t);
	}

	public InternalFailureException(String message) {
		super(message);
	}

	public static InternalFailureException of(Throwable t) {
		if (t instanceof InternalFailureException)
			return (InternalFailureException) t;
		else
			return new InternalFailureException(t);
	}

	public static InternalFailureException of(String message, Throwable t) {
		if (t instanceof InternalFailureException)
			return (InternalFailureException) t;
		else
			return new InternalFailureException(message, t);
	}
}