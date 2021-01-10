package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that can be used to pay for a transaction. It uses the ed25519 algorithm
 * for signing transactions.
 */
public class ExternallyOwnedAccountED25519 extends ExternallyOwnedAccount implements AccountED25519 {

	/**
	 * Creates an externally owned account with no funds.
	 * 
	 * @param publicKey the Base64-encoded ED25519 public key of the account
	 * @throws NullPointerException if {@code publicKey} is null
	 */
	public ExternallyOwnedAccountED25519(String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned account with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded ED25519 public key of the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountED25519(int initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned account with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded ED25519 public key of the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountED25519(long initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned account with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded ED25519 public key of the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountED25519(BigInteger initialAmount, String publicKey) {
		super(publicKey);
	}
}