package io.takamaka.code.instrumentation;

import io.takamaka.code.instrumentation.issues.Error;

public class VerificationException extends RuntimeException {
	private static final long serialVersionUID = -1232455923178336022L;
	private final io.takamaka.code.instrumentation.issues.Error error;

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
	public io.takamaka.code.instrumentation.issues.Error getError() {
		return error;
	}
}