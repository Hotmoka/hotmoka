package takamaka.lang;

import java.math.BigInteger;

import takamaka.whitelisted.WhiteListed;

/**
 * An exception thrown when a contract has not enough funds to
 * pay for funding another contract.
 */
@SuppressWarnings("serial")
public class InsufficientFundsError extends Error {

	@WhiteListed
	public InsufficientFundsError(BigInteger amount) {
		super("Missing " + amount + " coin units to invoke payable code");
	}
}