package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that can be used to pay for a transaction. It uses the sha256dsa algorithm
 * for signing transactions.
 */
public class ExternallyOwnedAccountSHA256DSA extends ExternallyOwnedAccount implements AccountSHA256DSA {

	/**
	 * Creates an externally owned account with no funds.
	 * 
	 * @param publicKey the Base64-encoded SHA256DSA public key of the account
	 * @throws NullPointerException if {@code publicKey} is null
	 */
	public ExternallyOwnedAccountSHA256DSA(String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned account with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded SHA256DSA public key of the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountSHA256DSA(int initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned account with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded SHA256DSA public key of the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountSHA256DSA(long initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned account with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded SHA256DSA public key of the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountSHA256DSA(BigInteger initialAmount, String publicKey) {
		super(publicKey);
	}
}