package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A red/green contract that can be used to pay for a transaction.
 * It is meant for tests, where it is useful to check for
 * the balances of a contract. Namely, these can be accessed
 * through the {@link io.takamaka.code.lang.TestRedGreenExternallyOwnedAccount#getBalance()}
 * and {@link io.takamaka.code.lang.TestRedGreenExternallyOwnedAccount#getBalanceRed()}
 * methods. Note that {@link io.takamaka.code.lang.Contract#balance()} and
 * {@link io.takamaka.code.lang.RedGreenContract#balanceRed()} are
 * protected and cannot be accessed freely in tests.
 */
public class TestRedGreenExternallyOwnedAccount extends RedGreenExternallyOwnedAccount {

	/**
	 * Creates an externally owned contract with no funds.
	 *
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	public TestRedGreenExternallyOwnedAccount(String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	@Payable @FromContract
	public TestRedGreenExternallyOwnedAccount(int initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	@Payable @FromContract
	public TestRedGreenExternallyOwnedAccount(long initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	@Payable @FromContract
	public TestRedGreenExternallyOwnedAccount(BigInteger initialAmount, String publicKey) {
		super(publicKey);
	}

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