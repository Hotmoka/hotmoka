package takamaka.lang;

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
	 * Used for the instrumentation of the entry constructors.
	 * To be removed in the future.
	 * TODO
	 */
	protected static Contract temp;

	/**
	 * Builds a contract with zero balance.
	 */
	@WhiteListed
	protected Contract() {
		this.balance = BigInteger.ZERO;
	}

	/**
	 * Yields the caller of the entry currently being executed.
	 * 
	 * @return the caller
	 */
	@WhiteListed
	protected final Contract caller() {
		return caller;
	}

	/**
	 * Yields the balance of this contract.
	 * 
	 * @return the balance
	 */
	@WhiteListed
	protected final BigInteger balance() {
		return balance;
	}

	/**
	 * Requires that the given condition holds.
	 * This is a synonym of {@link takamaka.lang.Contract#requireThat(boolean, String)}.
	 * 
	 * @param condition the condition that must hold
	 * @param message the message used in the exception raised if the
	 *                condition does not hold
	 * @throws RequirementViolationException if the condition does not hold
	 */
	@WhiteListed
	protected final void require(boolean condition, String message) {
		if (!condition)
			throw new RequirementViolationException(message);
	}

	/**
	 * Requires that the given condition holds.
	 * This is a synonym of {@link takamaka.lang.Contract#require(boolean, String)}.
	 * 
	 * @param condition the condition that must hold
	 * @param message the message used in the exception raised if the
	 *                condition does not hold
	 * @throws RequirementViolationException if the condition does not hold
	 */
	@WhiteListed
	protected final void requireThat(boolean condition, String message) {
		if (!condition)
			throw new RequirementViolationException(message);
	}

	/**
	 * Asserts that the given condition holds.
	 * 
	 * @param condition the condition that must hold
	 * @param message the message used in the exception raised if the
	 *                condition does not hold
	 * @throws AssertionViolationException if the condition does not hold
	 */
	@WhiteListed
	protected final void assertThat(boolean condition, String message) {
		if (!condition)
			throw new AssertionViolationException(message);
	}

	@WhiteListed @Override
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
		require(amount.signum() >= 0, "payed amount cannot be negative");
		if (balance.compareTo(amount) < 0)
			throw new InsufficientFundsError(amount);

		balance = balance.subtract(amount);
		beneficiary.balance = beneficiary.balance.add(amount);
	}

	/**
	 * Called at the beginning of the instrumentation of an entry method or constructor.
	 * It sets the caller of the entry.
	 * 
	 * @param caller the caller of the entry
	 */
	protected final void entry(Contract caller) {
		require(this != caller, "An @Entry can only be called from a distinct contract object");
		this.caller = caller;
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It sets the caller of the entry and transfers the amount of coins to the entry.
	 * 
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 */
	protected final void payableEntry(Contract caller, BigInteger amount) {
		entry(caller);
		caller.pay(this, amount);
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It sets the caller of the entry and transfers the amount of coins to the entry.
	 * 
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 */
	protected final void payableEntry(Contract caller, int amount) {
		payableEntry(caller, BigInteger.valueOf(amount));
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It sets the caller of the entry and transfers the amount of coins to the entry.
	 * 
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 */
	protected final void payableEntry(Contract caller, long amount) {
		payableEntry(caller, BigInteger.valueOf(amount));
	}
}