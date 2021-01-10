package io.hotmoka.beans.responses;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.updates.Update;

/**
 * A response for a transaction that installed a jar in an initialized node.
 */
@Immutable
public abstract class JarStoreNonInitialTransactionResponse extends NonInitialTransactionResponse implements JarStoreTransactionResponse {

	/**
	 * Builds the transaction response.
	 *
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public JarStoreNonInitialTransactionResponse(Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof JarStoreNonInitialTransactionResponse && super.equals(other);
	}

	/**
	 * Yields the outcome of the execution having this response, performed
	 * at the given transaction reference.
	 * 
	 * @param transactionReference the transaction reference
	 * @return the outcome
	 * @throws TransactionException if the outcome of the transaction is this exception
	 */
	public abstract TransactionReference getOutcomeAt(TransactionReference transactionReference) throws TransactionException;
}