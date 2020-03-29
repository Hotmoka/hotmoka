package io.hotmoka.beans.responses;

import java.math.BigInteger;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.values.StorageReference;

/**
 * A response for a transaction that should call a constructor of a storage class in blockchain.
 */
@Immutable
public abstract class ConstructorCallTransactionResponse extends CodeExecutionTransactionResponse {
	private static final long serialVersionUID = 6999069256965379003L;

	/**
	 * Builds the transaction response.
	 * 
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public ConstructorCallTransactionResponse(BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}

	/**
	 * Yields the outcome of the execution having this response.
	 * 
	 * @return the outcome
	 */
	public abstract StorageReference getOutcome() throws TransactionException, CodeExecutionException;
}