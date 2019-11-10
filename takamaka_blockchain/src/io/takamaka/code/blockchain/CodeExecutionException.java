package io.takamaka.code.blockchain;

/**
 * A wrapper of an exception that occurred during the execution of
 * a Takamaka constructor or method.
 */
@SuppressWarnings("serial")
public class CodeExecutionException extends Exception {

	public CodeExecutionException(String message, Throwable cause) {
		super(message, cause);
	}
}