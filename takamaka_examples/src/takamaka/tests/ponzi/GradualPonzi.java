package takamaka.tests.ponzi;

import static takamaka.lang.Takamaka.require;

import java.math.BigInteger;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.PayableContract;
import takamaka.util.StorageList;
import takamaka.util.StorageMap;

/**
 * A contract for a Ponzi investment scheme:
 * It involves taking the money sent by the current investor
 * and transferring it to the previous investors. Each investor
 * gets payed back gradually as soon as new investors arrive.
 * 
 * This example is translated from a Solidity contract shown
 * in "Building Games with Ethereum Smart Contracts", by Iyer and Dannen,
 * page 150, Apress 2018.
 */
public class GradualPonzi extends Contract {
	private static final BigInteger MINIMUM_INVESTMENT = BigInteger.valueOf(1_000);

	/**
	 * All investors up to now. This list might contain the same investor
	 * many times, which is important to pay it back more than investors
	 * who only invested ones. Hence this list is not the list of keys
	 * of the {@code balances} map, which does not account for repetitions.
	 */
	private final StorageList<PayableContract> investors = new StorageList<>();

	/**
	 * A map from each investor to the balance that he is allowed to withdraw.
	 */
	private final StorageMap<PayableContract, BigInteger> balances = new StorageMap<>();

	public @Entry(PayableContract.class) GradualPonzi() {
		investors.add((PayableContract) caller());
	}

	public @Payable @Entry(PayableContract.class) void invest(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "You must invest at least" + MINIMUM_INVESTMENT);
		BigInteger eachInvestorGets = amount.divide(BigInteger.valueOf(investors.size()));
		investors.stream().forEach(investor -> balances.update(investor, BigInteger.ZERO, eachInvestorGets::add));
		investors.add((PayableContract) caller());
	}

	public @Entry(PayableContract.class) void withdraw() {
		PayableContract payee = (PayableContract) caller();
		payee.receive(balances.getOrDefault(payee, BigInteger.ZERO));
		balances.put(payee, BigInteger.ZERO);
	}
}