package takamaka.blockchain;

import java.math.BigInteger;

public class OutOfGasException extends RuntimeException {
	public OutOfGasException(BigInteger amount) {
		super("Out of gas exception: " + amount);
	}
}