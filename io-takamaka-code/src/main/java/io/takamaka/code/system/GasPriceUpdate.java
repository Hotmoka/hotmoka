package io.takamaka.code.system;

import java.math.BigInteger;

import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.FromContract;

/**
 * An event issued when the price of the gas has changed.
 */
public class GasPriceUpdate extends Event {
	public final BigInteger newGasPrice;

	protected @FromContract GasPriceUpdate(BigInteger newGasPrice) {
		this.newGasPrice = newGasPrice;
	}
}