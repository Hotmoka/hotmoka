package io.hotmoka.nodes;

import java.util.stream.Stream;

/**
 * A wrapper of an exception that occurred during the execution of
 * a Takamaka constructor or method. It does not keep the
 * cause of the exception, since it would be created by another class loader
 * and might be a class that does not exist outside the Takamaka code.
 * It keeps just name and message of the cause. Its stack trace is a copy
 * of that of the cause.
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

	/**
	 * Builds an exception that occurred during the execution of a Takamaka constructor or method.
	 * 
	 * @param message the message of the exception
	 * @param classNameOfCause the name of the class of the cause of the exception
	 * @param messageOfCause the message of the cause of the exception
	 * @param stackTrace the stack trace of the cause of the exception
	 */
	public CodeExecutionException(String message, String classNameOfCause, String messageOfCause, Stream<StackTraceElement> stackTrace) {
		super(message + ' ' + classNameOfCause + (messageOfCause == null ? ":" : (": " + messageOfCause + ":")));

		this.classNameOfCause = classNameOfCause;
		this.messageOfCause = messageOfCause;

		// we use the stack trace of the code that threw the exception
		setStackTrace(stackTrace.toArray(StackTraceElement[]::new));
	}
}