package io.hotmoka.beans.responses;

import java.math.BigInteger;

import io.hotmoka.beans.responses.TransactionResponse;

/**
 * A response for a transaction that might have consumed gas.
 */
public interface TransactionResponseWithGas extends TransactionResponse {

	/**
	 * Yields the amount of gas that the transaction consumed for CPU execution.
	 * 
	 * @return the amount of gas
	 */
	BigInteger gasConsumedForCPU();

	/**
	 * Yields the amount of gas that the transaction consumed for RAM allocation.
	 * 
	 * @return the amount of gas
	 */
	BigInteger gasConsumedForRAM();

	/**
	 * Yields the amount of gas that the transaction consumed for storage consumption.
	 * 
	 * @return the amount of gas
	 */
	BigInteger gasConsumedForStorage();
}