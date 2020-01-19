package io.takamaka.code.blockchain;

/**
 * A wrapper of an exception that occurred during the execution of
 * a Takamaka constructor or method. It does not keep the
 * cause of the exception, since it will be created by another class loader
 * and my be a class that does not exist outside the Takamaka code.
 * It keeps just name, message and stack trace of the cause.
 */
@SuppressWarnings("serial")
public class CodeExecutionException extends Exception {

	/**
	 * The fully-qualified class name of the cause exception.
	 */
	public final String classNameOfCause;

	/**
	 * The message of the cause exception.
	 */
	public final String messageOfCause;

	public CodeExecutionException(String message, Throwable cause) {
		super(message + ' ' + cause.getClass().getName() + (cause.getMessage() == null ? ":" : (": " + cause.getMessage() + ":")));

		this.classNameOfCause = cause.getClass().getName();
		this.messageOfCause = cause.getMessage();

		// we use the stack trace of the code that threw the exception
		setStackTrace(cause.getStackTrace());
	}
}