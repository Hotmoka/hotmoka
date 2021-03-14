package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that can be used to pay for a transaction.
 * Its constructors allow one to create such a contract with an initial
 * amount of green coins. In order to initialize its red balance as well,
 * one can later call its {@link io.takamaka.code.lang.PayableContract#receiveRed(int)} method
 * or similar. It uses the sha256dsa algorithm for signing transactions.
 */
public class ExternallyOwnedAccountSHA256DSA extends ExternallyOwnedAccount implements AccountSHA256DSA {

	/**
	 * Creates an externally owned contract with no funds.
	 * 
	 * @param publicKey the Base64-encoded sha256dsa public key that will be assigned to the account
	 */
	public ExternallyOwnedAccountSHA256DSA(String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 * @param publicKey the Base64-encoded sha256dsa public key that will be assigned to the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountSHA256DSA(int initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 * @param publicKey the Base64-encoded sha256dsa public key that will be assigned to the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountSHA256DSA(long initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 * @param publicKey the Base64-encoded sha256dsa public key that will be assigned to the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountSHA256DSA(BigInteger initialAmount, String publicKey) {
		super(publicKey);
	}
}