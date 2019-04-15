package takamaka.lang;

import java.math.BigInteger;

public abstract class Contract extends Storage {
	private BigInteger balance;
	private transient Contract caller;

	// used for the instrumentation of @Entry constructors
	protected static Contract temp;

	protected Contract() {
		this.balance = BigInteger.ZERO;
	}

	protected final void require(boolean condition, String message) {
		if (!condition)
			throw new RequirementViolationException(message);
	}

	protected final void requireThat(boolean condition, String message) {
		if (!condition)
			throw new RequirementViolationException(message);
	}

	protected final void assertThat(boolean condition, String message) {
		if (!condition)
			throw new AssertionViolationException(message);
	}

	private void pay(Contract whom, BigInteger amount) {
		require(amount.signum() >= 0, "payed amount cannot be negative");
		if (balance.compareTo(amount) < 0)
			throw new InsufficientFundsError(amount);

		balance = balance.subtract(amount);
		whom.balance = whom.balance.add(amount);
	}

	protected final void entry(Contract caller) {
		require(this != caller, "An @Entry can only be called from a distinct contract object");
		this.caller = caller;
	}

	protected final void payableEntry(Contract caller, BigInteger amount) {
		entry(caller);
		caller.pay(this, amount);
	}

	protected final void payableEntry(Contract caller, int amount) {
		payableEntry(caller, BigInteger.valueOf(amount));
	}

	protected final void payableEntry(Contract caller, long amount) {
		payableEntry(caller, BigInteger.valueOf(amount));
	}

	protected final Contract caller() {
		return caller;
	}

	protected final BigInteger balance() {
		return balance;
	}

	@Override
	public String toString() {
		return "a contract";
	}
}