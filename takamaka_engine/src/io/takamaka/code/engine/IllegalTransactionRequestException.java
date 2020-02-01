package io.takamaka.code.engine;

import io.hotmoka.beans.TransactionException;

/**
 * An exception thrown when an illegal request is being processed.
 * Hence, its transaction could not be executed. For non-initial transactions,
 * when this exception is thrown, it was not even possible to generate
 * a failed transaction response, because the parameters of the request did not
 * allow to identify the payer or it has too few coins for paying for the
 * failed response.
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