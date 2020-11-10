package io.takamaka.tests.ponzi;

import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.util.ModifiableStorageList;
import io.takamaka.code.util.StorageMap;

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
public class SimplePyramidWithBalance extends Contract {
	public final BigInteger MINIMUM_INVESTMENT = BigInteger.valueOf(10_000L);
	private final ModifiableStorageList<PayableContract> investors = ModifiableStorageList.empty();
	private int previousLayerSize = 1;
	private final StorageMap<PayableContract, BigInteger> balances = new StorageMap<>();
	private BigInteger pyramidBalance;

	public @Payable @FromContract(PayableContract.class) SimplePyramidWithBalance(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "you must invest at least " + MINIMUM_INVESTMENT);
		investors.add((PayableContract) caller());
		pyramidBalance = amount;
	}

	public @Payable @FromContract(PayableContract.class) void invest(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "you must invest at least " + MINIMUM_INVESTMENT);
		pyramidBalance = pyramidBalance.add(amount);
		investors.add((PayableContract) caller());

		if (investors.size() == previousLayerSize * 4 - 1) {
			// pay out previous layer: note that currentLayer's size is even here
			investors.stream().skip(previousLayerSize - 1).limit(previousLayerSize).forEachOrdered(investor -> balances.update(investor, ZERO, MINIMUM_INVESTMENT::add));
			// spread remaining money among all participants
			BigInteger eachInvestorGets = pyramidBalance.subtract(MINIMUM_INVESTMENT.multiply(BigInteger.valueOf(previousLayerSize))).divide(BigInteger.valueOf(investors.size()));
			investors.forEach(investor -> balances.update(investor, ZERO, eachInvestorGets::add));
			pyramidBalance = ZERO;
			previousLayerSize *= 2;
		}
	}

	public @FromContract(PayableContract.class) void withdraw() {
		((PayableContract) caller()).receive(balances.getOrDefault(caller(), ZERO));
		balances.remove(caller());
	}
}