package takamaka.lang;

public final class ExternallyOwnedAccount extends PayableContract {

	/**
	 * Creates an externally owned contract with no funds.
	 */
	public ExternallyOwnedAccount() {}

	/**
	 * Creates an externally owned contract with the given initiali fund.
	 * 
	 * @param initialAmount the initial fund
	 */
	public @Entry @Payable ExternallyOwnedAccount(int initialAmount) {}

	@Override
	public String toString() {
		return "an externally owned account with balance " + balance();
	}
}