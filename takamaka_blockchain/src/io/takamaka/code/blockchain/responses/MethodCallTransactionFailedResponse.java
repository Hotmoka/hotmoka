package io.takamaka.code.blockchain.responses;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.takamaka.code.blockchain.TransactionException;

/**
 * A response for a failed transaction that should have called a method in blockchain.
 */
@Immutable
public class MethodCallTransactionFailedResponse extends MethodCallTransactionResponse implements TransactionResponseFailed {

	private static final long serialVersionUID = -4635934226304384321L;

	/**
	 * The exception that justifies why the transaction failed. This is not reported
	 * in the serialization of this response.
	 */
	public final transient TransactionException cause;

	/**
	 * The update of balance of the caller of the transaction, for paying for the transaction.
	 */
	private UpdateOfBalance callerBalanceUpdate;

	/**
	 * The amount of gas consumed by the transaction as penalty for the failure.
	 */
	private final BigInteger gasConsumedForPenalty;

	/**
	 * Builds the transaction response.
	 * 
	 * @param cause the exception that justifies why the transaction failed
	 * @param callerBalanceUpdate the update of balance of the caller of the transaction, for paying for the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @param gasConsumedForPenalty the amount of gas consumed by the transaction as penalty for the failure
	 */
	public MethodCallTransactionFailedResponse(TransactionException cause, UpdateOfBalance callerBalanceUpdate, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, BigInteger gasConsumedForPenalty) {
		super(gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.cause = cause;
		this.callerBalanceUpdate = callerBalanceUpdate;
		this.gasConsumedForPenalty = gasConsumedForPenalty;
	}

	@Override
	protected String gasToString() {
		return super.gasToString() + "  gas consumed for penalty: " + gasConsumedForPenalty + "\n";
	}

	@Override
	public BigInteger gasConsumedForPenalty() {
		return gasConsumedForPenalty;
	}

	@Override
	public Stream<Update> getUpdates() {
		return Stream.of(callerBalanceUpdate);
	}
}