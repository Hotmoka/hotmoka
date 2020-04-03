package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that can be used to pay for a transaction.
 */
public interface Account {

	/**
	 * Yields the current nonce of this account. If this account is used for paying
	 * a non-view transaction, the nonce in the request of the transaction must match
	 * this value, otherwise the transaction will be rejected.
	 * This value will be incremented at the end of any non-view transaction
	 * (also for unsuccessful transactions).
	 * 
	 * @return the current nonce of this account
	 */
	@View BigInteger nonce();
}