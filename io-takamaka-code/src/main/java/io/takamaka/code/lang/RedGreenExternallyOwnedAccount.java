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
	 * The Base64-encoded public key of the account.
	 */
	@SuppressWarnings("unused")
	private final String publicKey; // accessed by reflection

	/**
	 * Creates an externally owned contract with no funds.
	 * 
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	public RedGreenExternallyOwnedAccount(String publicKey) {
		this.publicKey = publicKey;
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	@Payable @Entry
	public RedGreenExternallyOwnedAccount(int initialAmount, String publicKey) {
		this.publicKey = publicKey;
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	@Payable @Entry
	public RedGreenExternallyOwnedAccount(long initialAmount, String publicKey) {
		this.publicKey = publicKey;
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	@Payable @Entry
	public RedGreenExternallyOwnedAccount(BigInteger initialAmount, String publicKey) {
		this.publicKey = publicKey;
	}

	@Override
	public String toString() {
		return "an externally owned red/green account";
	}

	@Override
	public @View BigInteger nonce() {
		return nonce;
	}
}