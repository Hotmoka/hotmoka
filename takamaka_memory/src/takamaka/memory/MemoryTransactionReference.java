package takamaka.memory;

import java.math.BigInteger;

import takamaka.blockchain.TransactionReference;
import takamaka.lang.Immutable;

/**
 * In the disk memory representation of a blockchain, a transaction can be
 * uniquely identified by a pair block/transaction inside the block. A progressive
 * identifier would also be fine.
 */
@Immutable
final class MemoryTransactionReference extends TransactionReference {

	/**
	 * The number of the block holding the transaction.
	 */
	final BigInteger blockNumber;

	/**
	 * The number of the transaction inside the block.
	 */
	final short transactionNumber;

	/**
	 * Builds a transaction reference.
	 * 
	 * @param blockNumber The number of the block holding the transaction.
	 * @param transactionNumber The number of the transaction inside the block.
	 */
	MemoryTransactionReference(BigInteger blockNumber, short transactionNumber) {
		this.blockNumber = blockNumber;
		this.transactionNumber = transactionNumber;
	}

	@Override
	public int compareTo(TransactionReference other) {
		MemoryTransactionReference otherAsTR = (MemoryTransactionReference) other;

		int diff = blockNumber.compareTo(otherAsTR.blockNumber);
		if (diff != 0)
			return diff;
		else
			return Short.compare(transactionNumber, otherAsTR.transactionNumber);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MemoryTransactionReference && ((MemoryTransactionReference) other).blockNumber.equals(blockNumber) && ((MemoryTransactionReference) other).transactionNumber == transactionNumber;
	}

	@Override
	public int hashCode() {
		return blockNumber.hashCode() ^ transactionNumber;
	}

	@Override
	public String toString() {
		return blockNumber.toString(16) + "." + Integer.toHexString(transactionNumber);
	}
}