package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that can be used to pay for a transaction. It uses the qtesla-p-I algorithm
 * for signing transactions.
 */
public class ExternallyOwnedAccountQTESLA1 extends ExternallyOwnedAccount implements AccountQTESLA1 {

	/**
	 * Creates an externally owned account with no funds.
	 * 
	 * @param publicKey the Base64-encoded QTESLA public key of the account
	 * @throws NullPointerException if {@code publicKey} is null
	 */
	public ExternallyOwnedAccountQTESLA1(String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned account with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded QTESLA public key of the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountQTESLA1(int initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned account with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded QTESLA public key of the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountQTESLA1(long initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned account with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded QTESLA public key of the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountQTESLA1(BigInteger initialAmount, String publicKey) {
		super(publicKey);
	}
}