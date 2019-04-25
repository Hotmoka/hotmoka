package takamaka.blockchain;

import java.io.Serializable;

import takamaka.lang.Immutable;

/**
 * A unique identifier for a transaction. This can be anything, from a
 * progressive number to a block/transaction pair to a database reference.
 * Each specific implementation of {@link takamaka.blockchain.Blockchain}
 * provides its implementation of this class.
 * They must be comparable and ordered according to their occurrence in the blockchain.
 */
@Immutable
public abstract class TransactionReference implements Serializable, Comparable<TransactionReference> {

	private static final long serialVersionUID = -9161931007616256670L;

	/**
	 * Determines if this transaction reference precedes the other one in the blockchain.
	 * 
	 * @param other the other blockchain reference
	 * @return true if and only if that condition holds
	 */
	public final boolean isOlderThan(TransactionReference other) {
		return compareTo(other) < 0;
	}

	@Override
	public abstract boolean equals(Object other);

	@Override
	public abstract int hashCode();

	@Override
	public abstract String toString();

	/**
	 * Yields the reference to the transaction that precedes this one.
	 * 
	 * @return the previous transaction reference, if any. Yields {@code null} if this
	 *         refers to the first transaction in blockchain
	 */
	protected abstract TransactionReference getPrevious();
}