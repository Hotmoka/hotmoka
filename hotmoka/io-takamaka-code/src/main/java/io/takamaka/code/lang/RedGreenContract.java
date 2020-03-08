package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that has a second balance, for red coins. Red coins can be
 * used for calling red payable methods.
 */
public abstract class RedGreenContract extends Contract {
	/**
	 * The red balance of this contract.
	 */
	private BigInteger balanceRed;

	/**
	 * Builds a contract with zero balance, for both normal coins
	 * and red coins.
	 */
	protected RedGreenContract() {
		this.balanceRed = BigInteger.ZERO;
	}

	/**
	 * Yields the normal, <i>green</i> balance of this contract.
	 * This is synonym for {@link #balance()}.
	 * 
	 * @return the balance
	 */
	protected final BigInteger balanceGreen() {
		return super.balance();
	}

	/**
	 * Yields the <i>red</i> balance of this contract.
	 * 
	 * @return the balance
	 */
	protected final BigInteger balanceRed() {
		return balanceRed;
	}

	@Override
	public String toString() {
		return "a red/green contract";
	}

	/**
	 * Increases the red balance of a contract by the given amount of coins,
	 * taken away from the balance of this contract.
	 * 
	 * @param beneficiary the beneficiary of the amount of red coins
	 * @param amount the amount of red coins
	 */
	private void payRed(RedGreenContract beneficiary, BigInteger amount) {
		Takamaka.require(amount != null, "Payed amount cannot be null");
		Takamaka.require(amount.signum() >= 0, "Payed amount cannot be negative");
		if (balanceRed.compareTo(amount) < 0)
			throw new InsufficientFundsError(amount);

		balanceRed = balanceRed.subtract(amount);
		beneficiary.balanceRed = beneficiary.balanceRed.add(amount);
	}

	/**
	 * Called at the beginning of the instrumentation of a red payable entry method or constructor.
	 * It transfers the amount of coins to the entry.
	 * It is private, so that programmers cannot call
	 * it directly. Instead, instrumented code will call it by reflection.
	 * 
	 * @param caller the caller of the entry
	 * @param amount the amount of red coins
	 */
	private void redPayable(RedGreenContract caller, BigInteger amount) {
		caller.payRed(this, amount);
	}

	/**
	 * Called at the beginning of the instrumentation of a red payable entry method or constructor.
	 * It transfers the amount of red coins to the entry.
	 * It is private, so that programmers cannot call
	 * it directly. Instead, instrumented code will call it by reflection.
	 *
	 * @param caller the caller of the entry
	 * @param amount the amount of red coins
	 */
	@SuppressWarnings("unused")
	private void redPayable(RedGreenContract caller, int amount) {
		redPayable(caller, BigInteger.valueOf(amount));
	}

	/**
	 * Called at the beginning of the instrumentation of a red payable entry method or constructor.
	 * It transfers the amount of red coins to the entry.
	 * It is private, so that programmers cannot call
	 * it directly. Instead, instrumented code will call it by reflection.
	 * 
	 * @param caller the caller of the entry
	 * @param amount the amount of red coins
	 */
	@SuppressWarnings("unused")
	private void redPayable(RedGreenContract caller, long amount) {
		redPayable(caller, BigInteger.valueOf(amount));
	}
}