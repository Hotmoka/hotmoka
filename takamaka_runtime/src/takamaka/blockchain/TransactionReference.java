package takamaka.blockchain;

import takamaka.lang.Immutable;

/**
 * A transaction reference is a reference to a transaction in the blockchain.
 */

@Immutable
public final class TransactionReference implements Comparable<TransactionReference> {

	/**
	 * The number of the block holding the transaction.
	 */
	public final long blockNumber;

	/**
	 * The number of the transaction inside the block.
	 */
	public final short transactionNumber;

	/**
	 * Builds a transaction reference.
	 * 
	 * @param blockNumber The number of the block holding the transaction.
	 * @param transactionNumber The number of the transaction inside the block.
	 */
	public TransactionReference(long blockNumber, short transactionNumber) {
		this.blockNumber = blockNumber;
		this.transactionNumber = transactionNumber;
	}

	@Override
	public int compareTo(TransactionReference other) {
		int diff = Long.compare(blockNumber, other.blockNumber);
		if (diff != 0)
			return diff;
		else
			return Short.compare(transactionNumber, other.transactionNumber);
	}

	public boolean isOlderThan(TransactionReference other) {
		return compareTo(other) < 0;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof TransactionReference && ((TransactionReference) other).blockNumber == blockNumber && ((TransactionReference) other).transactionNumber == transactionNumber;
	}

	@Override
	public int hashCode() {
		return ((int) blockNumber) ^ transactionNumber;
	}

	@Override
	public String toString() {
		return String.format("%016x%04x", blockNumber, transactionNumber);
	}
}