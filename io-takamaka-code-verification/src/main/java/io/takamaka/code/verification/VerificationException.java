package io.takamaka.code.verification;

import io.takamaka.code.verification.issues.Error;

public class VerificationException extends RuntimeException {
	private static final long serialVersionUID = -1232455923178336022L;
	private final Error error;

	public VerificationException() {
		this.error = null;
	}

	public VerificationException(Error error) {
		super(error.toString());

		this.error = error;
	}

	/**
	 * Yields the verification error that caused the exception.
	 * 
	 * @return the error
	 */
	public Error getError() {
		return error;
	}
}