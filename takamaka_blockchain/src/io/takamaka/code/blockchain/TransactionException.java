package io.takamaka.code.blockchain;

/**
 * An exception raised when a blockchain transaction cannot be completed.
 */
@SuppressWarnings("serial")
public class TransactionException extends Exception {
	public TransactionException(String message) {
		super(message);
	}

	public TransactionException(String message, Throwable cause) {
		super(message, cause);
	}
}