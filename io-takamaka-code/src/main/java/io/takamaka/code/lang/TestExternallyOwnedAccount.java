package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that can be used to pay for a transaction.
 * It is meant for tests, where it is useful to check for
 * the balance of a contract. Namely, this can be accessed
 * through the {@link io.takamaka.code.lang.TestExternallyOwnedAccount#getBalance()}
 * method. Note that {@link io.takamaka.code.lang.Contract#balance()} is
 * protected and cannot be accessed freely in tests.
 */
public class TestExternallyOwnedAccount extends ExternallyOwnedAccount {

	/**
	 * Creates an externally owned contract with no funds.
	 */
	public TestExternallyOwnedAccount() {}

	/**
	 * Creates an externally owned contract with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 */
	@Payable @Entry
	public TestExternallyOwnedAccount(int initialAmount) {}

	/**
	 * Creates an externally owned contract with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 */
	@Payable @Entry
	public TestExternallyOwnedAccount(long initialAmount) {}

	/**
	 * Creates an externally owned contract with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 */
	@Payable @Entry
	public TestExternallyOwnedAccount(BigInteger initialAmount) {}

	@Override
	public String toString() {
		return "an externally owned account with public balance";
	}

	/**
	 * Yields the balance of this contract.
	 * 
	 * @return the balance
	 */
	public @View BigInteger getBalance() {
		return balance();
	}
}