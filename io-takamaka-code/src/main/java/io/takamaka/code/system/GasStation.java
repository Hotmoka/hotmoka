package io.takamaka.code.system;

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.View;

/**
 * A contract that keeps track of the price of gas.
 */
public class GasStation extends Contract {

	/**
	 * The units of gas that we aim as average at each reward.
	 * If the actual reward is smaller, the price of gas must decrease.
	 * If it is larger, the price of gas must increase.
	 */
	public final BigInteger TARGET_GAS_AT_REWARD = BigInteger.valueOf(2_500L);

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
	 */
	GasStation(Manifest manifest) {
		this.manifest = manifest;
		this.pastGasConsumedWeighted = TARGET_GAS_AT_REWARD.multiply(COMPLEMENT_OF_OBLIVION);
		this.gasPrice = BigInteger.valueOf(100L); // initial attempt
		this.remainder = ZERO;
	}

	/**
	 * Takes note that the given gas has been consumed for CPU usage during the last reward iteration.
	 * 
	 * @param gasConsumed the amount of gas consumed for CPU usage, always non-negative
	 */
	protected @FromContract(Validators.class) void takeNoteOfGasConsumedForCpuDuringLastReward(BigInteger gasConsumed) {
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

		if (gasPrice.signum() == 0)
			gasPrice = ONE;

		event(new GasPriceChanged(gasPrice));
	}

	/**
	 * Yields the current gas price, that is, the units of coins
	 * necessary to buy a unit of gas.
	 * 
	 * @return the gas price, always positive
	 */
	public final @View BigInteger getGasPrice() {
		return gasPrice;
	}

	public static class GasPriceChanged extends Event {
		public final BigInteger newGasPrice;

		@FromContract(GasStation.class) GasPriceChanged(BigInteger newGasPrice) {
			this.newGasPrice = newGasPrice;
		}
	}

	/*public static void main(String[] args) throws InterruptedException {
		GasStation gs = new GasStation(null);
		java.util.Random random = new java.util.Random();

		while (true) {
			int gasConsumed = 900_000 + random.nextInt(200_000);
			gs.takeNoteOfGasConsumedForLastReward(BigInteger.valueOf(gasConsumed));
			System.out.println("gasConsumed " + gasConsumed + ", price: " + gs.gasPrice);
			Thread.sleep(100);
		}
	}*/
}