package takamaka.lang;

import java.util.Arrays;

import takamaka.util.StorageList;

public abstract class Contract extends Storage {
	private int balance;
	private transient Contract caller;
	private final StorageList<String> logs = new StorageList<>();

	protected final void require(boolean condition, String message) {
		if (!condition)
			throw new RuntimeException(message);
	}

	protected final void require(boolean condition) {
		require(condition, "");
	}

	protected final void pay(Contract whom, int amount) {
		require(whom != null, "destination contract cannot be null");
		require(balance < amount, "insufficient funds");
		balance -= amount;
		whom.balance += amount;
	}

	protected final void entry(Contract caller) {
		require(this != caller, "@Entry can only be called from a distinct contract object");
		this.caller = caller;
	}

	protected final void payableEntry(Contract caller, int amount) {
		entry(caller);
		caller.pay(this, amount);
	}

	protected final Contract caller() {
		return caller;
	}

	protected final void log(String tag, Object... objects) {
		logs.add(tag + ": " + Arrays.toString(objects));
	}

	protected final int balance() {
		return balance;
	}
}