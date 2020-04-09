package io.hotmoka.beans;

/**
 * An exception raised when a transaction has been started
 * but ended up into an exception inside the execution engine of the node,
 * not in the user code executed during the transaction (if any).
 */
@SuppressWarnings("serial")
public class TransactionException extends Exception {
	public TransactionException(String message) {
		super(message);
	}

	public TransactionException(Throwable cause) {
		super(cause.getClass().getName() + messageOf(cause), cause);
	}

	private static String messageOf(Throwable cause) {
		return cause.getMessage() == null ? "" : (": " + cause.getMessage());
	}
}