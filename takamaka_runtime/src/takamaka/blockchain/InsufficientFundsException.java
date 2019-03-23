package takamaka.blockchain;

public class InsufficientFundsException extends RuntimeException {
	public InsufficientFundsException() {
		super("Not enough funds");
	}
}