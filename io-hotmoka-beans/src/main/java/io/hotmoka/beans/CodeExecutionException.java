package io.hotmoka.beans;

/**
 * A wrapper of an exception that occurred during the execution of
 * a Takamaka constructor or method. The exception is inside
 * the user code being executed, not in the engine of the node
 * that is executing the code.
 */
@SuppressWarnings("serial")
public class CodeExecutionException extends Exception {

	/**
	 * The fully-qualified class name of the cause exception.
	 */
	public final String classNameOfCause;

	/**
	 * Builds an exception that occurred during the execution of a Takamaka constructor or method.
	 * 
	 * @param classNameOfCause the name of the class of the cause of the exception
	 * @param messageOfCause the message of the cause of the exception
	 * @param where a description of the program point of the exception
	 */
	public CodeExecutionException(String classNameOfCause, String messageOfCause, String where) {
		super(classNameOfCause
			+ (messageOfCause == null ? "" : (": " + messageOfCause))
			+ (where == null ? "" : "@" + where));

		this.classNameOfCause = classNameOfCause;
	}
}