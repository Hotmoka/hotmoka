package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that can receive funds from any other contract, through
 * its {@link io.takamaka.code.lang.PayableContract#receive(int)} method.
 * If this class is subclassed, the resulting contract has a balance
 * that is not determined by its payable methods only, but also by the
 * funds received through {@link io.takamaka.code.lang.PayableContract#receive(int)}.
 */
public abstract class PayableContract extends Contract {

	/**
	 * Receives the given amount of funds from the caller of the method.
	 * 
	 * @param amount the amount of funds
	 */
	@Payable @Entry
	public final void receive(int amount) {}

	/**
	 * Receives the given amount of funds from the caller of the method.
	 * 
	 * @param amount the amount of funds
	 */
	@Payable @Entry
	public final void receive(long amount) {}

	/**
	 * Receives the given amount of funds from the caller of the method.
	 * 
	 * @param amount the amount of funds
	 */
	@Payable @Entry
	public final void receive(BigInteger amount) {}
}