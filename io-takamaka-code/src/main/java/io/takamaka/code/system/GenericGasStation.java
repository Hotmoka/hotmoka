package io.takamaka.code.system;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.function.Function;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

/**
 * A generic implementation of a contract that keeps track of the price of gas.
 */
public class GenericGasStation extends Contract implements GasStation {

	/**
	 * The maximal gas limit that can be offered by a transaction request.
	 * Requests with higher gas limits will be rejected.
	 */
	public final BigInteger maxGasPerTransaction;

	/**
	 * True if and only if the node ignores the minimum gas price.
	 * Hence requests that specify a lower gas price
	 * than the current gas price of the node are executed anyway.
	 * This is mainly useful for testing. It defaults to false.
	 */
	public final boolean ignoresGasPrice;

	/**
	 * The units of gas that we aim as average at each reward.
	 * If the actual reward is smaller, the price of gas must decrease.
	 * If it is larger, the price of gas must increase.
	 */
	public final BigInteger TARGET_GAS_AT_REWARD = BigInteger.valueOf(10_000L);

	/**
	 * How quick the gas consumed at previous rewards is forgotten:
	 * 0 means never, {@link #MAX_OBLIVION} means immediately.
	 * Hence a smaller level means that the latest rewards are heavier
	 * in the determination of the gas price.
	 */
	public final BigInteger OBLIVION = BigInteger.valueOf(50_000L);

	/**
	 * The maximal value for {@link #OBLIVION}.
	 */
	public final BigInteger MAX_OBLIVION = BigInteger.valueOf(1_000_000L);

	private final BigInteger COMPLEMENT_OF_OBLIVION = MAX_OBLIVION.subtract(OBLIVION);

	private final BigInteger DIVISOR = TARGET_GAS_AT_REWARD.multiply(MAX_OBLIVION);

	/**
	 * The manifest of the node.
	 */
	private final Manifest manifest;

	/**
	 * The gas consumed in the past, a weight of the last rewards.
	 */
	private BigInteger pastGasConsumedWeighted;

	private BigInteger remainder;

	/**
	 * The current gas price, that is, the amount of coins necessary to buy
	 * a unit of gas. This is always strictly positive.
	 */
	private BigInteger gasPrice;

	/**
	 * Builds an object that keeps track of the price of the gas.
	 * 
	 * @param manifest the manifest of the node
	 * @param maxGasPerTransaction the maximal gas limit that can be offered by a transaction request.
	 *                             Requests with higher gas limits will be rejected.
	 */
	GenericGasStation(Manifest manifest, BigInteger maxGasPerTransaction, boolean ignoresGasPrice) {
		this.manifest = manifest;
		this.maxGasPerTransaction = maxGasPerTransaction;
		this.ignoresGasPrice = ignoresGasPrice;
		this.pastGasConsumedWeighted = TARGET_GAS_AT_REWARD.multiply(COMPLEMENT_OF_OBLIVION);
		this.gasPrice = BigInteger.valueOf(100L); // initial attempt
		this.remainder = ZERO;
	}

	@Override
	public @FromContract void takeNoteOfGasConsumedDuringLastReward(BigInteger gasConsumed) {
		require(caller() == manifest.validators, "only the validators can call this method");
		require(gasConsumed.signum() >= 0, "the gas consumed cannot be negative");

		// we give OBLIVION weight to the latest gas consumed and the complement to the past
		BigInteger weighted =
			gasConsumed.multiply(OBLIVION)
			.add(pastGasConsumedWeighted);

		pastGasConsumedWeighted = weighted.multiply(COMPLEMENT_OF_OBLIVION).divide(MAX_OBLIVION);

		BigInteger[] division = gasPrice.multiply(weighted)
			.add(remainder)
			.divideAndRemainder(DIVISOR);

		gasPrice = division[0];
		remainder = division[1];

		// the gas price must always be positive
		if (gasPrice.signum() == 0)
			gasPrice = ONE;

		event(new GasPriceChanged(gasPrice));
	}

	@Override
	public final @View BigInteger getMaxGasPerTransaction() {
		return maxGasPerTransaction;
	}

	@Override
	public final @View BigInteger getGasPrice() {
		return gasPrice;
	}

	@Override
	public final boolean ignoresGasPrice() {
		return ignoresGasPrice;
	}

	@Exported
	public static class Builder extends Storage implements Function<Manifest, GasStation> {

		private final BigInteger maxGasPerTransaction;
		private final boolean ignoresGasPrice;

		/**
		 * @param maxGasPerTransaction the maximal gas limit that can be offered by a transaction request.
		 *                             Requests with higher gas limits will be rejected.
		 */
		public Builder(BigInteger maxGasPerTransaction, boolean ignoresGasPrice) {
			this.maxGasPerTransaction = maxGasPerTransaction;
			this.ignoresGasPrice = ignoresGasPrice;
		}

		@Override
		public GasStation apply(Manifest manifest) {
			return new GenericGasStation(manifest, maxGasPerTransaction, ignoresGasPrice);
		}
	}
}