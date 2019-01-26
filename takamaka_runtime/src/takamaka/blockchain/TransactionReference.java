package takamaka.blockchain;

import takamaka.lang.Immutable;

/**
 * A transaction reference is a reference to a transaction in the blockchain.
 */

@Immutable
public final class TransactionReference {

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
	TransactionReference(long blockNumber, short transactionNumber, short progressive) {
		this.blockNumber = blockNumber;
		this.transactionNumber = transactionNumber;
	}
}