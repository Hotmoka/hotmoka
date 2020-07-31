package io.hotmoka.beans.responses;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.updates.Update;

/**
 * A response for a transaction that calls a constructor or method.
 */
@Immutable
public abstract class CodeExecutionTransactionResponse extends NonInitialTransactionResponse implements TransactionResponseWithGas, TransactionResponseWithUpdates {

	/**
	 * The updates resulting from the execution of the transaction.
	 */
	private final Update[] updates;

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
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public CodeExecutionTransactionResponse(Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		this.updates = updates.toArray(Update[]::new);
		this.gasConsumedForCPU = gasConsumedForCPU;
		this.gasConsumedForRAM = gasConsumedForRAM;
		this.gasConsumedForStorage = gasConsumedForStorage;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof CodeExecutionTransactionResponse) {
			CodeExecutionTransactionResponse otherCast = (CodeExecutionTransactionResponse) other;
			return Arrays.equals(updates, otherCast.updates) && gasConsumedForCPU.equals(gasConsumedForCPU)
				&& gasConsumedForRAM.equals(gasConsumedForRAM) && gasConsumedForStorage.equals(gasConsumedForStorage);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(updates) ^ gasConsumedForCPU.hashCode() ^ gasConsumedForRAM.hashCode() ^ gasConsumedForStorage.hashCode();
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
	public final Stream<Update> getUpdates() {
		return Stream.of(updates);
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
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel)
			.add(getUpdates().map(update -> update.size(gasCostModel)).reduce(BigInteger.ZERO, BigInteger::add))
			.add(gasCostModel.storageCostOf(gasConsumedForCPU))
			.add(gasCostModel.storageCostOf(gasConsumedForRAM))
			.add(gasCostModel.storageCostOf(gasConsumedForStorage));
	}

	@Override
	public void into(ObjectOutputStream oos) throws IOException {
		intoArray(updates, oos);
		marshal(gasConsumedForCPU, oos);
		marshal(gasConsumedForRAM, oos);
		marshal(gasConsumedForStorage, oos);
	}
}