package io.hotmoka.memory.internal;

import java.math.BigInteger;

import io.hotmoka.beans.references.TransactionReference;

/**
 * In the disk memory representation of a blockchain, a transaction can be
 * uniquely identified by a pair block/transaction inside the block. A progressive
 * identifier would also be fine.
 */
class MemoryTransactionReference implements TransactionReference {

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
	public int compareTo(TransactionReference other) {
		// this transaction reference is created by the memory blockchain only, that
		// generates only this kind of transaction references. Hence this cast must succeed
		MemoryTransactionReference otherAsTR = (MemoryTransactionReference) other;
	
		int diff = blockNumber.compareTo(otherAsTR.blockNumber);
		if (diff != 0)
			return diff;
		else
			return transactionNumber - otherAsTR.transactionNumber;
	}

	/**
	 * Yields the reference to the transaction that follows this one.
	 * 
	 * @return the next transaction reference
	 */
	MemoryTransactionReference getNext() {
		if (isLastInBlock())
			return new MemoryTransactionReference(blockNumber.add(BigInteger.ONE), (short) 0);
		else
			return new MemoryTransactionReference(blockNumber, (short) (transactionNumber + 1));
	}

	/**
	 * Determines if this transaction is the last in its block.
	 * 
	 * @return true if and only if this transaction is the last in its block
	 */
	boolean isLastInBlock() {
		return transactionNumber + 1 == AbstractMemoryBlockchain.TRANSACTIONS_PER_BLOCK;
	}
}