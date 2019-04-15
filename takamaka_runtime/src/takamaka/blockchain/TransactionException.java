package takamaka.blockchain;

/**
 * An exception raised when a blockchain transaction cannot be completed.
 */
public class TransactionException extends Exception {
	public TransactionException(String message) {
		super(message);
	}

	public TransactionException(String message, Throwable cause) {
		super(message, cause);
	}
}