package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A red/green contract that can be used to pay for a transaction.
 * It is meant for tests, where it is useful to check for
 * the balances of a contract. Namely, these can be accessed
 * through the {@link io.takamaka.code.lang.TestRedGreenExternallyOwnedAccount#getBalance()}
 * and {@link io.takamaka.code.lang.TestRedGreenExternallyOwnedAccount#getBalanceRed()}
 * methods. Note that {@link io.takamaka.code.lang.Contract#balance()} and
 * {@link io.takamaka.code.lang.Contract#balanceRed()} are
 * protected and cannot be accessed freely in tests.
 */
public class TestRedGreenExternallyOwnedAccount extends RedGreenExternallyOwnedAccount {

	/**
	 * Creates an externally owned contract with no funds.
	 */
	public TestRedGreenExternallyOwnedAccount() {}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 */
	@Payable @Entry
	public TestRedGreenExternallyOwnedAccount(int initialAmount) {}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 */
	@Payable @Entry
	public TestRedGreenExternallyOwnedAccount(long initialAmount) {}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 */
	@Payable @Entry
	public TestRedGreenExternallyOwnedAccount(BigInteger initialAmount) {}

	@Override
	public String toString() {
		return "an externally owned red/green account with public balances";
	}

	/**
	 * Yields the green balance of this contract.
	 * 
	 * @return the green balance
	 */
	public @View BigInteger getBalance() {
		return balance();
	}

	/**
	 * Yields the red balance of this contract.
	 * 
	 * @return the red balance
	 */
	public @View BigInteger getBalanceRed() {
		return balanceRed();
	}
}