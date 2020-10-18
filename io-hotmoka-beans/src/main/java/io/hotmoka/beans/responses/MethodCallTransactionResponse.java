package io.hotmoka.beans.responses;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageValue;

/**
 * A response for a transaction that should call a method in blockchain.
 */
@Immutable
public abstract class MethodCallTransactionResponse extends CodeExecutionTransactionResponse {

	/**
	 * True if and only if the called method was annotated as {@code @@SelfCharged}, hence the
	 * execution was charged to its receiver.
	 */
	public final boolean selfCharged;

	/**
	 * Builds the transaction response.
	 * 
	 * @param selfCharged true if and only if the called method was annotated as {@code @@SelfCharged}, hence the
	 *                    execution was charged to its receiver
	 * @param updates the updates resulting from the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public MethodCallTransactionResponse(boolean selfCharged, Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.selfCharged = selfCharged;
	}

	/**
	 * Yields the outcome of the execution having this response.
	 * 
	 * @return the outcome
	 * @throws CodeExecutionException if the transaction failed with an exception inside the user code in store,
	 *                                allowed to be thrown outside the store
	 * @throws TransactionException if the transaction failed with an exception outside the user code in store,
	 *                              or not allowed to be thrown outside the store
	 */
	public abstract StorageValue getOutcome() throws TransactionException, CodeExecutionException;
}