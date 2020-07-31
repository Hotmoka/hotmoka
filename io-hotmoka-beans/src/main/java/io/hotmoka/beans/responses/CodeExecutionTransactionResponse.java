package io.hotmoka.beans.responses;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.updates.Update;

/**
 * A response for a transaction that calls a constructor or method.
 */
@Immutable
public abstract class CodeExecutionTransactionResponse extends NonInitialTransactionResponse {

	/**
	 * Builds the transaction response.
	 * 
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	protected CodeExecutionTransactionResponse(Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof CodeExecutionTransactionResponse && super.equals(other);
	}
}