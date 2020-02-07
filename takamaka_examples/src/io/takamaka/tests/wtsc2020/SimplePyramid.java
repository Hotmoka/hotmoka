package io.takamaka.tests.wtsc2020;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageList;

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

	public @Payable @Entry(PayableContract.class) SimplePyramid(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "you must invest at least " + MINIMUM_INVESTMENT);
		investors.add((PayableContract) caller());
	}

	public @Payable @Entry(PayableContract.class) void invest(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "you must invest at least " + MINIMUM_INVESTMENT);
		investors.add((PayableContract) caller());

		if (investors.size() == previousLayerSize * 4 - 1) {
			// pay out previous layer: note that the current layer's size is even here
			investors.stream().skip(previousLayerSize - 1).limit(previousLayerSize).forEachOrdered(investor -> investor.receive(MINIMUM_INVESTMENT));
			// spread remaining money among all participants
			BigInteger eachInvestorGets = balance().divide(BigInteger.valueOf(investors.size()));
			investors.forEach(investor -> investor.receive(eachInvestorGets));
			previousLayerSize *= 2;
		}
	}

	public @View PayableContract mostFrequentInvestor() {
		Map<PayableContract, Integer> frequencies = new HashMap<>();
		int max = 0;
		PayableContract result = null;
		for (PayableContract investor: investors) {
			frequencies.putIfAbsent(investor, 0);
			int investments = frequencies.get(investor) + 1;
			frequencies.put(investor, investments);
			if (investments > max) {
				max = investments;
				result = investor;
			}
		}

		return result;
	}
}