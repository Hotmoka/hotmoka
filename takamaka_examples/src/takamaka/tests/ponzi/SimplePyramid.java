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
 * A contract for a pyramid investment scheme:
 * each layer is twice as large as the previous layer.
 * Each layer receives its investment back when the next layer fills.
 * The leftover money is then distributed among all participants.
 * 
 * This example is translated from a Solidity contract shown
 * in "Building Games with Ethereum Smart Contracts", by Iyer and Dannen,
 * page 155, Apress 2018.
 */
public class SimplePyramid extends Contract {
	public final BigInteger MINIMUM_INVESTMENT = BigInteger.valueOf(10_000L);
	private final StorageList<PayableContract> investors = new StorageList<>();
	private int previousLayerSize = 1;
	private final StorageMap<PayableContract, BigInteger> balances = new StorageMap<>();
	private BigInteger pyramidBalance;

	public @Payable @Entry(PayableContract.class) SimplePyramid(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "You must invest at least " + MINIMUM_INVESTMENT);
		investors.add((PayableContract) caller());
		pyramidBalance = amount;
	}

	public @Payable @Entry(PayableContract.class) void invest(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "You must invest at least " + MINIMUM_INVESTMENT);
		pyramidBalance = pyramidBalance.add(amount);
		investors.add((PayableContract) caller());

		if (investors.size() == previousLayerSize * 4 - 1) {
			// pay out previous layer: note that currentLayer's size is even here
			investors.stream().skip(previousLayerSize - 1).limit(previousLayerSize).forEach(investor -> balances.update(investor, BigInteger.ZERO, MINIMUM_INVESTMENT::add));
			// spread remaining money among all participants
			BigInteger eachInvestorGets = pyramidBalance.subtract(MINIMUM_INVESTMENT.multiply(BigInteger.valueOf(previousLayerSize))).divide(BigInteger.valueOf(investors.size()));
			investors.stream().forEach(investor -> balances.update(investor, BigInteger.ZERO, eachInvestorGets::add));
			pyramidBalance = BigInteger.ZERO;
			previousLayerSize *= 2;
		}
	}

	public @Entry(PayableContract.class) void withdraw() {
		((PayableContract) caller()).receive(balances.getOrDefault(caller(), BigInteger.ZERO));
		balances.remove(caller());
	}
}