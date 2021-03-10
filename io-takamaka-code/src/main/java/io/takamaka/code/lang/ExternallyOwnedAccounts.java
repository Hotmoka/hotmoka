package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A collector of @ExternallyOwnedAccount 's.
 */
public class ExternallyOwnedAccounts extends Accounts<ExternallyOwnedAccount> {

	/**
	 * Creates the container.
	 * 
	 * @param amount the total amount of coins distributed to the accounts that get created;
	 *               this must be the sum of all {@code balances}
	 * @param balances the initial, green balances of the accounts; they must be as many as the {@code publicKeys}
	 * @param publicKeys the Base64-encoded public keys of the accounts
	 */
	public @FromContract @Payable ExternallyOwnedAccounts(BigInteger amount, BigInteger[] balances, String[] publicKeys) {
		super(amount, balances, publicKeys);
	}

	/**
	 * Creates the container.
	 * 
	 * @param amount the total amount of coins distributed to the accounts that get created;
	 *               this must be the sum of all {@code balances}
	 * @param balances the initial balances of the accounts,
	 *               as a space-separated sequence of big integers; they must be as many
	 *               as there are public keys in {@code publicKeys}
	 * @param publicKeys the public keys of the accounts,
	 *                   as a space-separated sequence of Base64-encoded public keys
	 */
	public @FromContract @Payable ExternallyOwnedAccounts(BigInteger amount, String balances, String publicKeys) {
		super(amount, balances, publicKeys);
	}

	@Override
	protected ExternallyOwnedAccount mkAccount(BigInteger balance, String publicKey) {
		return new ExternallyOwnedAccount(balance, publicKey);
	}
}