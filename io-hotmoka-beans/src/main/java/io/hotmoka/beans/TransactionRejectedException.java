package io.hotmoka.beans;

/**
 * An exception raised when a transaction cannot even be started.
 * Typically, this means that the payer of the transaction cannot be identified
 * or it has not enough money to pay for a failed transaction or that its signature
 * is invalid.
 */
@SuppressWarnings("serial")
public class TransactionRejectedException extends Exception {
	public TransactionRejectedException(String message) {
		super(message);
	}

	public TransactionRejectedException(Throwable cause) {
		super(cause.getClass().getName() + messageOf(cause), cause);
	}

	private static String messageOf(Throwable cause) {
		return cause.getMessage() == null ? "" : (": " + cause.getMessage());
	}
}