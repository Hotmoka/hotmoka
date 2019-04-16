package takamaka.lang;

import java.math.BigInteger;

/**
 * An exception thrown when a transaction has not enough gas
 * to complete its computation.
 */
@SuppressWarnings("serial")
public class OutOfGasError extends Error {
	public OutOfGasError(BigInteger amount) {
		super("Missing " + amount + " gas units");
	}
}