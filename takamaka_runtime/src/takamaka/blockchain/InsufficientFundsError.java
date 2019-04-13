package takamaka.blockchain;

public class InsufficientFundsError extends Error {
	public InsufficientFundsError() {
		super("Not enough funds");
	}
}