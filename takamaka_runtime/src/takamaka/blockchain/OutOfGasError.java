package takamaka.blockchain;

import java.math.BigInteger;

public class OutOfGasError extends Error {
	public OutOfGasError(BigInteger amount) {
		super("Out of gas exception: " + amount);
	}
}