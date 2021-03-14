package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that can be used to pay for a transaction.
 * Its constructors allow one to create such a contract with an initial
 * amount of green coins. In order to initialize its red balance as well,
 * one can later call its {@link io.takamaka.code.lang.PayableContract#receiveRed(int)} method
 * or similar. It uses the qtesla-p-I algorithm for signing transactions.
 */
public class ExternallyOwnedAccountQTESLA1 extends ExternallyOwnedAccount implements AccountQTESLA1 {

	/**
	 * Creates an externally owned contract with no funds.
	 * 
	 * @param publicKey the Base64-encoded qtesla public key that will be assigned to the account
	 */
	public ExternallyOwnedAccountQTESLA1(String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 * @param publicKey the Base64-encoded qtesla public key that will be assigned to the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountQTESLA1(int initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 * @param publicKey the Base64-encoded qtesla public key that will be assigned to the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountQTESLA1(long initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial green funds
	 * @param publicKey the Base64-encoded qtesla public key that will be assigned to the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountQTESLA1(BigInteger initialAmount, String publicKey) {
		super(publicKey);
	}
}