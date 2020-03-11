package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that can be used to pay for a transaction.
 */
public class ExternallyOwnedAccount extends PayableContract {

	/**
	 * Creates an externally owned contract with no funds.
	 */
	public ExternallyOwnedAccount() {}

	/**
	 * Creates an externally owned contract with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 */
	@Payable @Entry
	public ExternallyOwnedAccount(int initialAmount) {}

	/**
	 * Creates an externally owned contract with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 */
	@Payable @Entry
	public ExternallyOwnedAccount(long initialAmount) {}

	/**
	 * Creates an externally owned contract with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 */
	@Payable @Entry
	public ExternallyOwnedAccount(BigInteger initialAmount) {}

	@Override
	public String toString() {
		return "an externally owned account";
	}
}