package takamaka.lang;

import java.math.BigInteger;

/**
 * A contract that can be used to pay for a transaction.
 */
public class ExternallyOwnedAccount extends PayableContract {

	/**
	 * Creates an externally owned contract with no funds.
	 */
	@WhiteListed
	public ExternallyOwnedAccount() {}

	/**
	 * Creates an externally owned contract with the given initiali fund.
	 * 
	 * @param initialAmount the initial fund
	 */
	@WhiteListed @Payable @Entry
	public ExternallyOwnedAccount(int initialAmount) {}

	/**
	 * Creates an externally owned contract with the given initiali fund.
	 * 
	 * @param initialAmount the initial fund
	 */
	@WhiteListed @Payable @Entry
	public ExternallyOwnedAccount(long initialAmount) {}

	/**
	 * Creates an externally owned contract with the given initiali fund.
	 * 
	 * @param initialAmount the initial fund
	 */
	@WhiteListed @Payable @Entry
	public ExternallyOwnedAccount(BigInteger initialAmount) {}

	@WhiteListed @Override
	public String toString() {
		return "an externally owned account with balance " + balance();
	}
}