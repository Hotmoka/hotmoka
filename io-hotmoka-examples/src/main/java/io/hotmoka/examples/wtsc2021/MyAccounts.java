package io.hotmoka.examples.wtsc2021;

import java.math.BigInteger;
import java.util.Comparator;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.TestExternallyOnwedAccounts;
import io.takamaka.code.lang.TestExternallyOwnedAccount;
import io.takamaka.code.lang.View;

public class MyAccounts extends TestExternallyOnwedAccounts {
	
	/**
	 * Creates the container, for normal accounts.
	 * 
	 * @param amount the total amount of coins distributed to the accounts that get created;
	 *                this must be the sum of all {@code balances}
	 * @param balances the initial, green balances of the accounts; they must be as many as the {@code publicKeys}
	 * @param publicKeys the Base64-encoded public keys of the accounts
	 */
	public @FromContract @Payable MyAccounts(BigInteger amount, BigInteger[] balances, String[] publicKeys) {
		super(amount, balances, publicKeys);
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
	public @FromContract @Payable MyAccounts(BigInteger amount, String balances, String publicKeys) {
		super(amount, balances, publicKeys);
	}

	/**
	 * Yields the richest account in this container.
	 * 
	 * @return the richest account, or {@code null} is this container is empty
	 */
	public @View TestExternallyOwnedAccount richest() {
		return isEmpty() ? null : stream().max(Comparator.comparing(TestExternallyOwnedAccount::getBalance)).get();
	}
}