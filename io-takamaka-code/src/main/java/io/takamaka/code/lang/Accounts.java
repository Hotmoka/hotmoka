package io.takamaka.code.lang;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import io.takamaka.code.util.StorageIntMap;
import io.takamaka.code.util.StorageTreeIntMap;

/**
 * A collector of accounts.
 *
 * @param <A> the type of the accounts contained in this collector
 */
public abstract class Accounts<A extends Account> extends Contract implements Iterable<A> {

	/**
	 * The accounts contained in this container, in order of creation.
	 */
	private final StorageIntMap<A> accounts;

	/**
	 * Creates the container, for normal accounts.
	 * 
	 * @param amount the total amount of coins distributed to the accounts that get created;
	 *                this must be the sum of all {@code balances}
	 * @param balances the initial, green balances of the accounts; they must be as many as the {@code publicKeys}
	 * @param publicKeys the Base64-encoded public keys of the accounts
	 */
	protected @FromContract @Payable Accounts(BigInteger amount, BigInteger[] balances, String[] publicKeys) {
		require(balances != null, "balances cannot be null");
		require(publicKeys != null, "the public keys cannot be null");
		int length = balances.length;
		require(length == publicKeys.length, "the balances must be as many as the public keys");
		require(amount.equals(Stream.of(balances).reduce(BigInteger.ZERO, BigInteger::add)),
			"the amount payed for creating this collector must be equal to the sum of the balances of the accounts being created");

		this.accounts = new StorageTreeIntMap<>();
		for (int pos = 0; pos < length; pos++)
			accounts.put(pos, mkAccount(balances[pos], publicKeys[pos]));
	}

	/**
	 * Creates the container, for normal accounts.
	 * 
	 * @param amount the total amount of coins distributed to the accounts that get created;
	 *                this must be the sum of all {@code balances}
	 * @param balances the initial balances of the accounts,
	 *               as a space-separated sequence of big integers; they must be as many
	 *               as there are public keys in {@code publicKeys}
	 * @param publicKeys the public keys of the accounts,
	 *                   as a space-separated sequence of Base64-encoded public keys
	 */
	protected @FromContract @Payable Accounts(BigInteger amount, String balances, String publicKeys) {
		this(amount, buildBalances(balances), buildPublicKeys(publicKeys));
	}

	protected abstract A mkAccount(BigInteger balance, String publicKey);

	private static BigInteger[] buildBalances(String balancesAsStringSequence) {
		return splitAtSpaces(balancesAsStringSequence).stream()
			.map(BigInteger::new)
			.toArray(BigInteger[]::new);
	}

	private static String[] buildPublicKeys(String publicKeysAsStringSequence) {
		return splitAtSpaces(publicKeysAsStringSequence).toArray(String[]::new);
	}

	private static List<String> splitAtSpaces(String s) {
		List<String> list = new ArrayList<>();
		int pos;
		while ((pos = s.indexOf(' ')) >= 0) {
			list.add(s.substring(0, pos));
			s = s.substring(pos + 1);
		}

		if (!s.isEmpty())
			list.add(s);

		return list;
	}

	/**
	 * Yields an iterator over the accounts in this collector.
	 * 
	 * @return the iterator
	 */
	public final Iterator<A> iterator() {
		return stream().iterator();
	}

	/**
	 * Yields the accounts in this collector.
	 * 
	 * @return the accounts
	 */
	public final Stream<A> stream() {
		return accounts.values();
	}

	/**
	 * Yields the number of accounts in this collector.
	 * 
	 * @return the number of accounts
	 */
	public final int size() {
		return accounts.size();
	}

	/**
	 * Checks if this collector is empty.
	 * 
	 * @return true if and only if this collector is empty
	 */
	public final boolean isEmpty() {
		return accounts.isEmpty();
	}

	/**
	 * Yields the {@code key}th account in this collector, in the same order as balances and
	 * public keys have been passed to the constructor.
	 * 
	 * @param key the number of the account, from 0 inclusive to {@code size()} exclusive
	 * @return the account
	 */
	public final A get(int key) {
		return accounts.get(key);
	}
}