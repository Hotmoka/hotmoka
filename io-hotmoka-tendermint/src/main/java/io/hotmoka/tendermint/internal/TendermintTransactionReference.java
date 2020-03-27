package io.hotmoka.tendermint.internal;

import java.math.BigInteger;

import io.hotmoka.beans.references.TransactionReference;

/**
 * In Tendermint, a transaction can be uniquely identified by a pair block/transaction inside the block.
 * A progressive identifier would also be fine.
 */
class TendermintTransactionReference implements TransactionReference {

	private static final long serialVersionUID = 5563481038557431039L;

	/**
	 * The number of the block holding the transaction.
	 */
	final BigInteger blockNumber;

	/**
	 * The number of the transaction inside the block.
	 */
	final int transactionNumber;

	/**
	 * Builds a transaction reference.
	 * 
	 * @param blockNumber the number of the block holding the transaction
	 * @param transactionNumber the number of the transaction inside the block
	 */
	TendermintTransactionReference(BigInteger blockNumber, int transactionNumber) {
		this.blockNumber = blockNumber;
		this.transactionNumber = transactionNumber;
	}

	/**
	 * Builds a transaction reference from its string representation.
	 * 
	 * @param toString the string representation, exactly as it would result by
	 *                 calling {@code toString()} on the constructed transaction
	 *                 reference
	 */
	TendermintTransactionReference(String toString) {
		int dot = toString.indexOf('.');
		this.blockNumber = new BigInteger(toString.substring(0, dot), 16);		
		this.transactionNumber = Short.parseShort(toString.substring(dot + 1), 16);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof TendermintTransactionReference &&
			((TendermintTransactionReference) other).blockNumber.equals(blockNumber) &&
			((TendermintTransactionReference) other).transactionNumber == transactionNumber;
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
		// this transaction reference is created by the Tendermint blockchain only, that
		// generates only this kind of transaction references. Hence this cast must succeed
		TendermintTransactionReference otherAsTR = (TendermintTransactionReference) other;
	
		int diff = blockNumber.compareTo(otherAsTR.blockNumber);
		if (diff != 0)
			return diff;
		else
			return transactionNumber - otherAsTR.transactionNumber;
	}
}