package io.hotmoka.nodes;

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
	 * A description of the place of the exception.
	 */
	public final String where;

	/**
	 * Builds an exception that occurred during the execution of a Takamaka constructor or method.
	 * 
	 * @param message the message of the exception
	 * @param classNameOfCause the name of the class of the cause of the exception
	 * @param messageOfCause the message of the cause of the exception
	 * @param where a description of the program point of the exception
	 */
	public CodeExecutionException(String message, String classNameOfCause, String messageOfCause, String where) {
		super(message + ' ' + classNameOfCause + (messageOfCause == null ? "" : (": " + messageOfCause)) + "@" + where);

		this.classNameOfCause = classNameOfCause;
		this.messageOfCause = messageOfCause;
		this.where = where;
	}
}