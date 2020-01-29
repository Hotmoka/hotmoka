package io.takamaka.code.engine;

import io.hotmoka.beans.TransactionException;

/**
 * An exception thrown when an illegal request is being executed on a blockchain.
 */
@SuppressWarnings("serial")
public class IllegalTransactionRequestException extends TransactionException {

	public IllegalTransactionRequestException(String message) {
		super(message);
	}

	public IllegalTransactionRequestException(Throwable cause) {
		super(cause);
	}
}