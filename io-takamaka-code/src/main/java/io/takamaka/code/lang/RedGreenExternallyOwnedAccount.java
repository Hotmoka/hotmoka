package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A red/green contract that can be used to pay for a transaction.
 * Its constructors allow one to create such a contract with an initial
 * amount of green coins. In order to initialize its red balance as well,
 * one can later call its {@link io.takamaka.code.lang.RedGreenPayableContract#receiveRed(int)} method
 * or similar.
 */
public class RedGreenExternallyOwnedAccount extends RedGreenPayableContract implements Account {

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
	public RedGreenExternallyOwnedAccount() {}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 */
	@Payable @Entry
	public RedGreenExternallyOwnedAccount(int initialAmount) {}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 */
	@Payable @Entry
	public RedGreenExternallyOwnedAccount(long initialAmount) {}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 */
	@Payable @Entry
	public RedGreenExternallyOwnedAccount(BigInteger initialAmount) {}

	@Override
	public String toString() {
		return "an externally owned red/green account";
	}

	@Override
	public @View BigInteger nonce() {
		return nonce;
	}
}