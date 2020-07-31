package io.hotmoka.beans.responses;

import java.math.BigInteger;

/**
 * The response of a failed transaction. This means that the transaction
 * could not be executed until its end. All gas provided to the
 * transaction has been consumed, as a form of penalty.
 */
public interface TransactionResponseFailed {

	/**
	 * Yields the amount of gas that the transaction consumed for penalty, since it failed.
	 * 
	 * @return the amount of gas
	 */
	BigInteger gasConsumedForPenalty();

	/**
	 * Yields the fully-qualified class name of the cause exception.
	 */
	String getClassNameOfCause();

	/**
	 * Yields the message of the cause exception.
	 */
	String getMessageOfCause();
}