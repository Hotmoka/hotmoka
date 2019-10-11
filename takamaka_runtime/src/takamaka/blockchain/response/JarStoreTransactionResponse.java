package takamaka.blockchain.response;

import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import takamaka.blockchain.GasCosts;
import takamaka.blockchain.Update;
import takamaka.blockchain.UpdateOfBalance;
import takamaka.lang.Immutable;

/**
 * A response for a transaction that should install a jar in the blockchain.
 */
@Immutable
public abstract class JarStoreTransactionResponse implements TransactionResponseWithGas, TransactionResponseWithUpdates {

	private static final long serialVersionUID = -8888957484092351352L;

	/**
	 * The update of balance of the caller of the transaction, for paying for the transaction.
	 */
	private final UpdateOfBalance callerBalanceUpdate;

	/**
	 * The amount of gas consumed by the transaction for CPU execution.
	 */
	private final BigInteger gasConsumedForCPU;

	/**
	 * The amount of gas consumed by the transaction for RAM allocation.
	 */
	private final BigInteger gasConsumedForRAM;

	/**
	 * The amount of gas consumed by the transaction for storage consumption.
	 */
	private final BigInteger gasConsumedForStorage;

	/**
	 * Builds the transaction response.
	 *
	 * @param callerBalanceUpdate the update of balance of the caller of the transaction, for paying for the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public JarStoreTransactionResponse(UpdateOfBalance callerBalanceUpdate, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		this.callerBalanceUpdate = callerBalanceUpdate;
		this.gasConsumedForCPU = gasConsumedForCPU;
		this.gasConsumedForRAM = gasConsumedForRAM;
		this.gasConsumedForStorage = gasConsumedForStorage;
	}

	@Override
	public final Stream<Update> getUpdates() {
		return Stream.of(callerBalanceUpdate);
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n" + gasToString()
        	+ "  updates:\n" + getUpdates().map(Update::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	/**
	 * Yields a description of the gas consumption.
	 * 
	 * @return the description
	 */
	protected String gasToString() {
		return "  gas consumed for CPU execution: " + gasConsumedForCPU + "\n"
			+ "  gas consumed for RAM allocation: " + gasConsumedForRAM + "\n"
	        + "  gas consumed for storage consumption: " + gasConsumedForStorage + "\n";
	}

	@Override
	public BigInteger gasConsumedForCPU() {
		return gasConsumedForCPU;
	}

	@Override
	public BigInteger gasConsumedForRAM() {
		return gasConsumedForRAM;
	}

	@Override
	public BigInteger gasConsumedForStorage() {
		return gasConsumedForStorage;
	};

	@Override
	public BigInteger size() {
		return GasCosts.STORAGE_COST_PER_SLOT.add(callerBalanceUpdate.size()).add(GasCosts.storageCostOf(gasConsumedForCPU)).add(GasCosts.storageCostOf(gasConsumedForStorage));
	}
}