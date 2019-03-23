package takamaka.blockchain;

public class OutOfGasException extends RuntimeException {
	public OutOfGasException(long amount) {
		super("Out of gas exception: " + amount);
	}
}