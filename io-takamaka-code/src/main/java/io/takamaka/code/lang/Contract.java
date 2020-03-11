package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract is a storage object with a balance of coin. It is controlled
 * by the methods of its code.
 */
public abstract class Contract extends Storage {

	/**
	 * The balance of this contract.
	 */
	private BigInteger balance;

	/**
	 * The caller of the entry method or constructor currently
	 * being executed. This is set at the beginning of an entry and refers
	 * to the contract that called the entry.
	 */
	private transient Contract caller;

	/**
	 * Builds a contract with zero balance.
	 */
	protected Contract() {
		this.balance = BigInteger.ZERO;
	}

	/**
	 * Yields the caller of the entry currently being executed.
	 * 
	 * @return the caller
	 */
	protected final Contract caller() {
		return caller;
	}

	/**
	 * Yields the balance of this contract.
	 * 
	 * @return the balance
	 */
	protected final BigInteger balance() {
		return balance;
	}

	@Override
	public String toString() {
		return "a contract";
	}

	/**
	 * Increases the balance of a contract by the given amount of coins,
	 * taken away from the balance of this contract.
	 * 
	 * @param beneficiary the beneficiary of the amount of coins
	 * @param amount the amount of coins
	 */
	private void pay(Contract beneficiary, BigInteger amount) {
		Takamaka.require(amount != null, "Payed amount cannot be null");
		Takamaka.require(amount.signum() >= 0, "Payed amount cannot be negative");
		if (balance.compareTo(amount) < 0)
			throw new InsufficientFundsError(amount);

		balance = balance.subtract(amount);
		beneficiary.balance = beneficiary.balance.add(amount);
	}

	/**
	 * Called at the beginning of the instrumentation of an entry method or constructor.
	 * It sets the caller of the entry. It is private, so that programmers cannot call
	 * it directly. Instead, instrumented code will call it by reflection.
	 * 
	 * @param caller the caller of the entry
	 */
	private void entry(Contract caller) {
		Takamaka.require(this != caller, "An @Entry can only be called from a distinct contract object");
		this.caller = caller;
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It sets the caller of the entry and transfers the amount of coins to the entry.
	 * It is private, so that programmers cannot call
	 * it directly. Instead, instrumented code will call it by reflection.
	 * 
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 */
	private void payableEntry(Contract caller, BigInteger amount) {
		entry(caller);
		caller.pay(this, amount);
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It sets the caller of the entry and transfers the amount of coins to the entry.
	 * It is private, so that programmers cannot call
	 * it directly. Instead, instrumented code will call it by reflection.
	 *
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 */
	@SuppressWarnings("unused")
	private void payableEntry(Contract caller, int amount) {
		payableEntry(caller, BigInteger.valueOf(amount));
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It sets the caller of the entry and transfers the amount of coins to the entry.
	 * It is private, so that programmers cannot call
	 * it directly. Instead, instrumented code will call it by reflection.
	 * 
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 */
	@SuppressWarnings("unused")
	private void payableEntry(Contract caller, long amount) {
		payableEntry(caller, BigInteger.valueOf(amount));
	}
}