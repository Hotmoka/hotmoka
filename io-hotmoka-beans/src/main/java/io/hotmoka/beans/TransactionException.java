package io.hotmoka.beans;

/**
 * A wrapper of an exception, raised during a transaction, that didn't occur during the execution
 * of a Takamaka constructor or method, or that did well occur inside it,
 * but the constructor or method wasn't allowed to throw it.
 */
@SuppressWarnings("serial")
public class TransactionException extends Exception {

	/**
	 * Builds an exception that didn't occur during the execution of a Takamaka constructor or method,
	 * or that did well occur inside it, but the constructor or method wasn't allowed to throw it.
	 * 
	 * @param classNameOfCause the name of the class of the cause of the exception
	 * @param messageOfCause the message of the cause of the exception. This might be {@code null}
	 * @param where a description of the program point of the exception. This might be {@code null}
	 */
	public TransactionException(String classNameOfCause, String messageOfCause, String where) {
		super(classNameOfCause
			+ (messageOfCause.isEmpty() ? "" : (": " + messageOfCause))
			+ (where.isEmpty() ? "" : "@" + where));
	}
}