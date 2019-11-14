package io.takamaka.code.blockchain;

/**
 * A transaction reference for a blockchain where transactions are executed
 * immediately and stacked sequentially. They are comparable and ordered
 * according to their occurrence in the blockchain
 * (older transactions come before newer ones).
 */
public abstract class SequentialTransactionReference implements TransactionReference {

	private static final long serialVersionUID = 367515181596412034L;

	@Override
	public final boolean isOlderThan(TransactionReference other) {
		return compareTo(other) < 0;
	}

	/**
	 * Yields the reference to the transaction that precedes this one.
	 * 
	 * @return the previous transaction reference, if any. Yields {@code null} if this
	 *         refers to the first transaction in blockchain
	 */
	public abstract SequentialTransactionReference getPrevious();
}