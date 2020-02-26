package io.takamaka.code.engine;

import io.hotmoka.beans.references.TransactionReference;

/**
 * A transaction reference for a sequential node where transactions are executed
 * and stacked sequentially. Hence it is possible to know which transaction reference
 * is older than another.
 */
public interface SequentialTransactionReference extends TransactionReference {

	/**
	 * Yields the reference to the transaction that precedes this one.
	 * 
	 * @return the previous transaction reference, if any. Yields {@code null} if this
	 *         refers to the first transaction in blockchain
	 */
	SequentialTransactionReference getPrevious();

	/**
	 * Determines if this transaction reference precedes the other one in the node's store.
	 * 
	 * @param other the other transaction reference
	 * @return true if and only if that condition holds
	 */
	boolean isOlderThan(TransactionReference other);
}