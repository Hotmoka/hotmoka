package takamaka.memory;

import takamaka.blockchain.TransactionReference;
import takamaka.lang.Immutable;

/**
 * In the disk memory representation of a blockchain, a transaction can be
 * uniquely identified by a pair block/transaction inside the block. A progressive
 * identifier would also be fine.
 */
@Immutable
public final class MemoryTransactionReference extends TransactionReference {

	/**
	 * The number of the block holding the transaction. Note that a {@code long}
	 * does not support infinite blockchains. However, this is only a test implementation
	 * of blockchain.
	 */
	public final long blockNumber;

	/**
	 * The number of the transaction inside the block. Note that a {@code short}
	 * does not support blockchains with really many transactions inside a single block.
	 * However, this is only a test implementation of blockchain.
	 */
	public final short transactionNumber;

	/**
	 * Builds a transaction reference.
	 * 
	 * @param blockNumber The number of the block holding the transaction.
	 * @param transactionNumber The number of the transaction inside the block.
	 */
	public MemoryTransactionReference(long blockNumber, short transactionNumber) {
		this.blockNumber = blockNumber;
		this.transactionNumber = transactionNumber;
	}

	/**
	 * Builds a transaction reference from a string. The format of the string is the
	 * same that would be returned by {@code toString()}. Hence
	 * {@code t.equals(new TransactionReference(t.toString()))} holds for
	 * every {@code TransactionReference t}.
	 * 
	 * @param s the string
	 * @throws NumberFormatException if the format of the string does not correspond
	 *                               to a {@link takamaka.memory.MemoryTransactionReference}
	 */
	public MemoryTransactionReference(String s) throws NumberFormatException {
		if (s == null || s.length() != 20)
			throw new NumberFormatException("Illegal transaction reference format: " + s);

		String blockPart = s.substring(0, 16);
		String transactionPart = s.substring(16);
		
		this.blockNumber = Long.decode("0x" + blockPart);
		this.transactionNumber = Short.decode("0x" + transactionPart);
	}

	@Override
	public int compareTo(TransactionReference other) {
		MemoryTransactionReference otherAsTR = (MemoryTransactionReference) other;

		int diff = Long.compare(blockNumber, otherAsTR.blockNumber);
		if (diff != 0)
			return diff;
		else
			return Short.compare(transactionNumber, otherAsTR.transactionNumber);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MemoryTransactionReference && ((MemoryTransactionReference) other).blockNumber == blockNumber && ((MemoryTransactionReference) other).transactionNumber == transactionNumber;
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