package takamaka.lang;

import java.util.Arrays;

import takamaka.util.StorageList;

public abstract class Contract extends Storage {
	private int balance;
	private transient Contract payer;
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

	protected final void entry(Contract payer) {
		require(this != payer, "@Entry can only be called from a distinct contract object");
		this.payer = payer;
	}

	protected final void payableEntry(Contract payer, int amount) {
		entry(payer);
		payer.pay(this, amount);
	}

	protected final Contract payer() {
		return payer;
	}

	protected final void log(String tag, Object... objects) {
		logs.add(tag + ": " + Arrays.toString(objects));
	}

	protected final int balance() {
		return balance;
	}
}