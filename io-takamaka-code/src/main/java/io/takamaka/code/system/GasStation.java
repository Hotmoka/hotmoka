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
	 * If the actual rewards are smaller, then the price of gas must decrease.
	 * If they are larger, then the price of gas must increase.
	 */
	public final BigInteger TARGET_GAS_AT_REWARD = BigInteger.valueOf(1_000L);

	/**
	 * How quick the gas consumed at previous rewards is forgotten.
	 * 10_000 means never. 0 means immediately.
	 * Hence a smaller level means that the latest rewards are heavier
	 * in the determination of the gas price.
	 */
	public final BigInteger OBLIVION = BigInteger.valueOf(2_000L);

	/**
	 * The manifest of the node.
	 */
	private final Manifest manifest;

	/**
	 * The sum of all the rewards, giving more weights to the latest.
	 */
	private BigInteger rewardsCumulative;

	/**
	 * The current gas price, that is, the amount of coins necessary to buy
	 * a unit of gas. This is always strictly positive.
	 */
	private BigInteger gasPrice;

	private final BigInteger _100 = BigInteger.valueOf(100L);

	/**
	 * Builds an object that keeps track of the price of the gas.
	 * 
	 * @param manifest the manifest of the node
	 */
	GasStation(Manifest manifest) {
		this.manifest = manifest;
		this.rewardsCumulative = ZERO;
		this.gasPrice = ONE;
	}

	/**
	 * Takes note that the given amount of coins has been distributed to the validators
	 * during the last reward iteration.
	 * 
	 * @param reward the amount of coins, always positive
	 */
	protected @FromContract(Validators.class) void takeNoteOfReward(BigInteger reward) {
		require(caller() == manifest.validators, "only the validators can call this method");
		require(reward.signum() >= 0, "the reward cannot be negative");

		BigInteger rewardAsGas = reward.divide(gasPrice);

		// we add the rewardAsGas to the cumulative rewards, reducing the weight of previous rewards,
		// in a way inversely proportion to OBLIVION
		rewardsCumulative = rewardsCumulative.multiply(OBLIVION).divide(BigInteger.valueOf(10_000L));
		rewardsCumulative = rewardsCumulative.add(rewardAsGas);

		// according to the geometric series, if the reward is constantly TARGET_GAS_AT_REWARD
		// then lastRewardsCumulative is 100 and the gas is at the right price; otherwise, we increase
		// or decrease the price of the gas
		gasPrice = gasPrice.multiply(rewardsCumulative).divide(_100);
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
}