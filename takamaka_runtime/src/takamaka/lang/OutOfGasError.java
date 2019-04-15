package takamaka.lang;

import java.math.BigInteger;

/**
 * An exception thrown when a contract has not enough funds to
 * pay for the required gas.
 */
public class OutOfGasError extends Error {
	public OutOfGasError(BigInteger amount) {
		super("Missing " + amount + " gas units");
	}
}