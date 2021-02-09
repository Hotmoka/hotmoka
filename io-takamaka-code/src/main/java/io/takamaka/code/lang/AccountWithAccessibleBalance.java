package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that can be used to pay for a transaction and whose
 * balance is freely accessible, through a public method (normally, it is protected).
 */
public interface AccountWithAccessibleBalance extends Account {

	/**
	 * Yields the balance (green) of the account.
	 * 
	 * @return the green balance
	 */
	@View BigInteger getBalance();
}