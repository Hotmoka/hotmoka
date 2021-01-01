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
	 * The Base64-encoded public key of the account, that can be used to check
	 * signatures of requests signed on its behalf.
	 */
	private final String publicKey;

	/**
	 * Creates an externally owned account with no initial funds.
	 * 
	 * @param publicKey the Base64-encoded public key of the account
	 * @throws NullPointerException if {@code publicKey} is null
	 */
	public ExternallyOwnedAccount(String publicKey) {
		if (publicKey == null)
			throw new NullPointerException("cannot set the public key to null");

		this.publicKey = publicKey;
	}

	/**
	 * Creates an externally owned account with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded public key of the account
	 * @throws NullPointerException if {@code publicKey} is null
	 */
	@Payable @FromContract
	public ExternallyOwnedAccount(int initialAmount, String publicKey) {
		this(publicKey);
	}

	/**
	 * Creates an externally owned account with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded public key of the account
	 * @throws NullPointerException if {@code publicKey} is null
	 */
	@Payable @FromContract
	public ExternallyOwnedAccount(long initialAmount, String publicKey) {
		this(publicKey);
	}

	/**
	 * Creates an externally owned account with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded public key of the account
	 * @throws NullPointerException if {@code publicKey} is null
	 */
	@Payable @FromContract
	public ExternallyOwnedAccount(BigInteger initialAmount, String publicKey) {
		this(publicKey);
	}

	@Override
	public String toString() {
		return "an externally owned account";
	}

	@Override
	public @View final BigInteger nonce() {
		return nonce;
	}

	/**
	 * Yields the public key of this account.
	 * 
	 * @return the public key
	 */
	protected final String publicKey() {
		return publicKey;
	}
}