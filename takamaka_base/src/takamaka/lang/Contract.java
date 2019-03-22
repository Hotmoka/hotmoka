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
			throw new RuntimeException(message);
	}

	private void pay(Contract whom, int amount) {
		require(amount >= 0, "payed amount cannot be negative");
		BigInteger amountAsBI = BigInteger.valueOf(amount);
		require(balance.compareTo(amountAsBI) >= 0, "insufficient funds");
		balance = balance.subtract(amountAsBI);
		whom.balance = whom.balance.add(amountAsBI);
	}

	protected final void entry(Contract caller) {
		require(this != caller, "An @Entry can only be called from a distinct contract object");
		this.caller = caller;
	}

	protected final void payableEntry(Contract caller, int amount) {
		entry(caller);
		caller.pay(this, amount);
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