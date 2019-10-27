package takamaka.blockchain.response;

import java.math.BigInteger;
import java.util.stream.Collectors;

import io.takamaka.annotations.Immutable;
import takamaka.blockchain.GasCosts;
import takamaka.blockchain.Update;

/**
 * A response for a transaction that should call a constructor of a storage class in blockchain.
 */
@Immutable
public abstract class ConstructorCallTransactionResponse implements TransactionResponseWithGas, TransactionResponseWithUpdates {

	private static final long serialVersionUID = 6999069256965379003L;

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
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public ConstructorCallTransactionResponse(BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		this.gasConsumedForCPU = gasConsumedForCPU;
		this.gasConsumedForRAM = gasConsumedForRAM;
		this.gasConsumedForStorage = gasConsumedForStorage;
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
	}

	@Override
	public BigInteger size() {
		return GasCosts.STORAGE_COST_PER_SLOT.add(GasCosts.storageCostOf(gasConsumedForCPU)).add(GasCosts.storageCostOf(gasConsumedForRAM)).add(GasCosts.storageCostOf(gasConsumedForStorage))
			.add(getUpdates().map(Update::size).reduce(BigInteger.ZERO, BigInteger::add));
	}
}