package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that can be used to pay for a transaction.
 * Its constructors allow one to create such a contract with an initial
 * amount of coins. In order to initialize its red balance as well,
 * one can later call its {@link io.takamaka.code.lang.PayableContract#receiveRed(int)} method
 * or similar.
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
	 * The Base64-encoded public key of the account.
	 */
	private final String publicKey;

	/**
	 * Creates an externally owned contract with no funds.
	 * 
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	public ExternallyOwnedAccount(String publicKey) {
		this.publicKey = publicKey;
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial funds
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	@Payable @FromContract
	public ExternallyOwnedAccount(int initialAmount, String publicKey) {
		this.publicKey = publicKey;
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial funds
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	@Payable @FromContract
	public ExternallyOwnedAccount(long initialAmount, String publicKey) {
		this.publicKey = publicKey;
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial funds
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	@Payable @FromContract
	public ExternallyOwnedAccount(BigInteger initialAmount, String publicKey) {
		this.publicKey = publicKey;
	}

	@Override
	public @View String toString() {
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
	public final String publicKey() {
		return publicKey;
	}
}