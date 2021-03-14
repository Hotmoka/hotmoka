package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that can receive funds from any other contract, through
 * its {@link io.takamaka.code.lang.PayableContract#receive(int)} method.
 * If this class is subclassed, the resulting contract has a balance
 * that is not determined by its payable methods only, but also by the
 * funds received through such receiving methods.
 */
public abstract class PayableContract extends Contract {

	/**
	 * Receives the given amount of coins from the caller of the method.
	 * 
	 * @param amount the amount of coins
	 */
	@Payable @FromContract
	public final void receive(int amount) {}

	/**
	 * Receives the given amount of coins from the caller of the method.
	 * 
	 * @param amount the amount of coins
	 */
	@Payable @FromContract
	public final void receive(long amount) {}

	/**
	 * Receives the given amount of coins from the caller of the method.
	 * 
	 * @param amount the amount of coins
	 */
	@Payable @FromContract
	public final void receive(BigInteger amount) {}

	/**
	 * Receives the given amount of red coins from the caller of the method.
	 * 
	 * @param amount the amount of red coins
	 */
	@RedPayable @FromContract
	public final void receiveRed(int amount) {}

	/**
	 * Receives the given amount of red coins from the caller of the method.
	 * 
	 * @param amount the amount of red coins
	 */
	@RedPayable @FromContract
	public final void receiveRed(long amount) {}

	/**
	 * Receives the given amount of red coins from the caller of the method.
	 * 
	 * @param amount the amount of red coins
	 */
	@RedPayable @FromContract
	public final void receiveRed(BigInteger amount) {}
}