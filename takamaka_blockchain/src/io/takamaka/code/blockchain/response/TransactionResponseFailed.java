package io.takamaka.code.blockchain.response;

import java.math.BigInteger;

/**
 * The response of a failed transaction. This means that the transaction
 * could not be executed until its end, all its updates have been reverted and
 * only the balance of the caller has been updated. All gas provided to the
 * transaction has been consumed, as penalty.
 */
public interface TransactionResponseFailed extends TransactionResponseWithGas, TransactionResponseWithUpdates {

	/**
	 * Yields the amount of gas that the transaction consumed for penalty, since it failed.
	 * 
	 * @return the amount of gas
	 */
	BigInteger gasConsumedForPenalty();
}