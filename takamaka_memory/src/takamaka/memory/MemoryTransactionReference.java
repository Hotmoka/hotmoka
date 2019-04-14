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
public final class MemoryTransactionReference extends TransactionReference {

	/**
	 * The number of the block holding the transaction.
	 */
	public final BigInteger blockNumber;

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
	public MemoryTransactionReference(BigInteger blockNumber, short transactionNumber) {
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
		int dollarPos;
		if (s == null || (dollarPos = s.indexOf('$')) < 0)
			throw new NumberFormatException("Illegal transaction reference format: " + s);

		String blockPart = s.substring(0, dollarPos);
		String transactionPart = s.substring(dollarPos + 1);
		
		this.blockNumber = new BigInteger(blockPart, 16);
		this.transactionNumber = Short.decode("0x" + transactionPart);
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
		return String.format("%s$%04x", blockNumber.toString(16), transactionNumber);
	}
}