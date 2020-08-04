package io.hotmoka.beans;

/**
 * An exception raised when a transaction cannot even be started.
 * Typically, this means that the payer of the transaction cannot be identified
 * or it has not enough money to pay for a failed transaction or that its signature
 * is invalid.
 */
@SuppressWarnings("serial")
public class TransactionRejectedException extends Exception {

	/**
	 * Builds an exception with the given message.
	 * 
	 * @param message the message
	 */
	public TransactionRejectedException(String message) {
		super(message);
	}

	/**
	 * Builds an exception with the given cause.
	 * 
	 * @param cause the cause
	 */
	public TransactionRejectedException(Throwable cause) {
		super(cause.getClass().getName() + messageOf(cause), cause);
	}

	private static String messageOf(Throwable cause) {
		return cause.getMessage() == null ? "" : (": " + cause.getMessage());
	}
}