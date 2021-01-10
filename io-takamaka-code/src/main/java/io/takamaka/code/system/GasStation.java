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
	 * Takes note that the given gas has been consumed during the last reward iteration.
	 * 
	 * @param gasConsumed the amount of gas consumed, always non-negative
	 */
	@FromContract void takeNoteOfGasConsumedDuringLastReward(BigInteger gasConsumed);

	/**
	 * Yields the current gas price, that is, the units of coins necessary to buy a unit of gas.
	 * 
	 * @return the gas price, always positive
	 */
	@View BigInteger getGasPrice();

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