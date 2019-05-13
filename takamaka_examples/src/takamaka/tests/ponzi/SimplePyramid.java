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
	public final BigInteger MINIMUM_INVESTMENT = BigInteger.valueOf(1_000L);
	private final StorageList<PayableContract> oldLayers = new StorageList<>();
	private StorageList<PayableContract> currentLayer = new StorageList<>();
	private final StorageMap<PayableContract, BigInteger> balances = new StorageMap<>();
	private BigInteger pyramidBalance = BigInteger.ZERO;

	public @Payable @Entry(PayableContract.class) SimplePyramid(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "You must invest at least " + MINIMUM_INVESTMENT);
		oldLayers.add((PayableContract) caller());
		pyramidBalance = amount;
	}

	public @Payable @Entry(Payable.class) void invest(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "You must invest at least " + MINIMUM_INVESTMENT);
		pyramidBalance = pyramidBalance.add(amount);
		currentLayer.add((PayableContract) caller());

		if (currentLayer.size() == oldLayers.size() + 1) {
			// pay out previous layer: note that currentLayer's size is even here
			oldLayers.stream().skip(currentLayer.size() / 2 - 1).forEach(investor -> balances.update(investor, BigInteger.ZERO, MINIMUM_INVESTMENT::add));
			// move current layer into oldLayers
			currentLayer.forEach(oldLayers::add);
			// spread remaining money among all participants
			BigInteger eachInvestorGets = pyramidBalance.subtract(MINIMUM_INVESTMENT.multiply(BigInteger.valueOf(currentLayer.size() / 2))).divide(BigInteger.valueOf(oldLayers.size()));
			oldLayers.stream().forEach(investor -> balances.update(investor, BigInteger.ZERO, eachInvestorGets::add));
			pyramidBalance = BigInteger.ZERO;
			// reset current layer
			currentLayer = new StorageList<>();
		}
	}

	public @Entry(PayableContract.class) void withdraw() {
		((PayableContract) caller()).receive(balances.get(caller()));
		balances.put((PayableContract) caller(), BigInteger.ZERO);
	}
}