package io.takamaka.code.system;

import java.math.BigInteger;

import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.View;

/**
 * An object that keeps track of the price of gas.
 */
public interface GasStation {

	/**
	 * The maximal value for {@link #getOblivion()}.
	 */
	final long MAX_OBLIVION = 1_000_000L;

	/**
	 * Takes note that the given gas has been consumed during the last reward iteration.
	 * 
	 * @param gasConsumed the amount of gas consumed, always non-negative
	 */
	@FromContract void takeNoteOfGasConsumedDuringLastReward(BigInteger gasConsumed);

	/**
	 * Yields the maximal gas limit that can be offered by a transaction request.
	 * Requests with higher gas limits will be rejected.
	 * 
	 * @return the maximal gas limit that can be offered by a transaction request
	 */
	@View BigInteger getMaxGasPerTransaction();

	/**
	 * Yields the units of gas that are aimed to be rewarded at each reward.
	 * If the actual reward is smaller, the price of gas must decrease.
	 * If it is larger, the price of gas must increase.
	 */
	@View BigInteger getTargetGasAtReward();

	/**
	 * Informs about how quick the gas consumed at previous rewards is forgotten:
	 * 0 means never, {@link #MAX_OBLIVION} means immediately.
	 * Hence a smaller level means that the latest rewards are heavier
	 * in the determination of the gas price.
	 */
	@View long getOblivion();

	/**
	 * Yields the current gas price, that is, the units of coins necessary to buy a unit of gas.
	 * 
	 * @return the gas price, always positive
	 */
	@View BigInteger getGasPrice();

	/**
	 * Determine if the gas price of the requests must be ignored, so that
	 * all requests are run, also when they offer a smaller gas price than the
	 * current gas price of the node. This is normally false.
	 * 
	 * @return true if and only if the gas price offered by requests must be ignored
	 */
	@View boolean ignoresGasPrice();

	/**
	 * An event issued when the price of the gas has changed.
	 */
	public static class GasPriceChanged extends Event {
		public final BigInteger newGasPrice;

		protected @FromContract GasPriceChanged(BigInteger newGasPrice) {
			this.newGasPrice = newGasPrice;
		}
	}
}