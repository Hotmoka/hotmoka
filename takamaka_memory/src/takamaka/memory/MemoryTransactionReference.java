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

	private static final long serialVersionUID = 5911713300386882185L;

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
	 * @param blockNumber the number of the block holding the transaction
	 * @param transactionNumber the number of the transaction inside the block
	 */
	MemoryTransactionReference(BigInteger blockNumber, short transactionNumber) {
		this.blockNumber = blockNumber;
		this.transactionNumber = transactionNumber;
	}

	@Override
	public int compareTo(TransactionReference other) {
		// this transaction reference is created by the memory blockchain only,
		// that generates only this kind of transaction references. Hence
		// this cast must succeed
		MemoryTransactionReference otherAsTR = (MemoryTransactionReference) other;

		int diff = blockNumber.compareTo(otherAsTR.blockNumber);
		if (diff != 0)
			return diff;
		else
			return Short.compare(transactionNumber, otherAsTR.transactionNumber);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MemoryTransactionReference &&
			((MemoryTransactionReference) other).blockNumber.equals(blockNumber) &&
			((MemoryTransactionReference) other).transactionNumber == transactionNumber;
	}

	@Override
	public int hashCode() {
		return blockNumber.hashCode() ^ transactionNumber;
	}

	@Override
	public String toString() {
		return blockNumber.toString(16) + "." + Integer.toHexString(transactionNumber);
	}

	@Override
	protected MemoryTransactionReference getPrevious() {
		if (transactionNumber == 0)
			if (blockNumber.signum() == 0)
				return null;
			else
				return new MemoryTransactionReference(blockNumber.subtract(BigInteger.ONE), (short) (MemoryBlockchain.TRANSACTION_PER_BLOCK - 1));
		else
			return new MemoryTransactionReference(blockNumber, (short) (transactionNumber - 1));
	}

	/**
	 * Yields the reference to the transaction that follows this one.
	 * 
	 * @return the next transaction reference
	 */
	protected MemoryTransactionReference getNext() {
		if (transactionNumber + 1 == MemoryBlockchain.TRANSACTION_PER_BLOCK)
			return new MemoryTransactionReference(blockNumber.add(BigInteger.ONE), (short) 0);
		else
			return new MemoryTransactionReference(blockNumber, (short) (transactionNumber + 1));
	}
}