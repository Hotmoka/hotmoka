package takamaka.lang;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import takamaka.blockchain.Update;
import takamaka.blockchain.values.StorageReference;
import takamaka.util.StorageList;

public abstract class Contract extends Storage {
	private BigInteger balance;
	private BigInteger oldBalance;
	// used for the instrumentation of @Entry constructors
	protected static Contract temp;
	private transient Contract caller;
	private final StorageList<String> logs = new StorageList<>();
	private final static String CONTRACT_CLASS_NAME = Contract.class.getName();

	protected Contract() {
		this.balance = BigInteger.ZERO;
	}

	/**
	 * Constructor for deserialization.
	 */
	protected Contract(StorageReference storageReference, BigInteger balance) {
		super(storageReference);

		this.balance = this.oldBalance = balance;
	}

	protected final void require(boolean condition, String message) {
		if (!condition)
			throw new RuntimeException(message);
	}

	protected final void pay(Contract whom, int amount) {
		require(whom != null, "destination contract cannot be null");
		require(amount >= 0, "payed amount cannot be negative");
		BigInteger amountAsBI = BigInteger.valueOf(amount);
		require(balance.compareTo(amountAsBI) < 0, "insufficient funds");
		balance = balance.subtract(amountAsBI);
		whom.balance = whom.balance.add(amountAsBI);
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

	protected final BigInteger balance() {
		return balance;
	}

	@Override
	protected void extractUpdates(Set<Update> updates, Set<StorageReference> seen, List<Storage> workingSet) {
		super.extractUpdates(updates, seen, workingSet);
		if (!inStorage || balance != oldBalance)
			addUpdateFor(CONTRACT_CLASS_NAME, "balance", updates, balance);

		// subclasses will override, call this super-implementation and add potential updates to their instance fields
	}
}