package io.hotmoka.beans;

/**
 * An exception raised when a transaction cannot be completed.
 */
@SuppressWarnings("serial")
public class TransactionException extends Exception {
	public TransactionException(String message) {
		super(message);
	}

	public TransactionException(Throwable cause) {
		super(cause);
	}
}