package takamaka.blockchain.response;

import java.math.BigInteger;
import java.util.stream.Stream;

import takamaka.blockchain.TransactionException;
import takamaka.blockchain.UpdateOfBalance;
import takamaka.lang.Immutable;

/**
 * A response for a failed transaction that should have installed a jar in the blockchain.
 */
@Immutable
public class JarStoreTransactionFailedResponse extends JarStoreTransactionResponse implements TransactionResponseFailed {

	private static final long serialVersionUID = -8888957484092351352L;

	/**
	 * The exception that justifies why the transaction failed. This is not reported
	 * in the serialization of this response.
	 */
	public final transient TransactionException cause;

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
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @param gasConsumedForPenalty the amount of gas consumed by the transaction as penalty for the failure
	 */
	public JarStoreTransactionFailedResponse(TransactionException cause, UpdateOfBalance callerBalanceUpdate, BigInteger gasConsumedForCPU, BigInteger gasConsumedForStorage, BigInteger gasConsumedForPenalty) {
		super(Stream.of(callerBalanceUpdate), gasConsumedForCPU, gasConsumedForStorage);

		this.cause = cause;
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
}