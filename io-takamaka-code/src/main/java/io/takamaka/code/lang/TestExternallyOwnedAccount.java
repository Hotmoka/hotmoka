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
public class TestExternallyOwnedAccount extends ExternallyOwnedAccount implements AccountWithAccessibleBalance {

	/**
	 * Creates an externally owned contract with no funds.
	 * 
	 * @param publicKey the Base64-encoded public key of the account
	 */
	public TestExternallyOwnedAccount(String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned contract with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded public key of the account
	 */
	@Payable @FromContract
	public TestExternallyOwnedAccount(int initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned contract with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded public key of the account
	 */
	@Payable @FromContract
	public TestExternallyOwnedAccount(long initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned contract with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded public key of the account
	 */
	@Payable @FromContract
	public TestExternallyOwnedAccount(BigInteger initialAmount, String publicKey) {
		super(publicKey);
	}

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