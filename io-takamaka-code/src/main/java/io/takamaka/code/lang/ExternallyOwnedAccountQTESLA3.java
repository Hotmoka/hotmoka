package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that can be used to pay for a transaction. It uses the qtesla-p-III algorithm
 * for signing transactions.
 */
public class ExternallyOwnedAccountQTESLA3 extends ExternallyOwnedAccount implements AccountQTESLA1 {

	/**
	 * Creates an externally owned account with no funds.
	 * 
	 * @param publicKey the Base64-encoded QTESLA public key of the account
	 * @throws NullPointerException if {@code publicKey} is null
	 */
	public ExternallyOwnedAccountQTESLA3(String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned account with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded QTESLA public key of the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountQTESLA3(int initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned account with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded QTESLA public key of the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountQTESLA3(long initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates an externally owned account with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded QTESLA public key of the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccountQTESLA3(BigInteger initialAmount, String publicKey) {
		super(publicKey);
	}
}