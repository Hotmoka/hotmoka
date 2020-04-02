package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that can be used to pay for a transaction.
 */
public class ExternallyOwnedAccount extends PayableContract implements Account {

	/**
	 * The current nonce of this account. If this account is used for paying
	 * a transaction, the nonce in the request of the transaction must match
	 * this value, otherwise the transaction will be rejected.
	 * This value will be incremented at the end of any transaction
	 * (also for unsuccessful transactions).
	 */
	private BigInteger nonce = BigInteger.ZERO;

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

	@Override
	public @View BigInteger nonce() {
		return nonce;
	}
}