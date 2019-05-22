package takamaka.tests.ponzi;

import static takamaka.lang.Takamaka.require;

import java.math.BigInteger;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.PayableContract;
import takamaka.util.StorageList;

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
public class SimplePyramidDirectPay extends Contract {
	public final BigInteger MINIMUM_INVESTMENT = BigInteger.valueOf(10_000L);
	private final StorageList<PayableContract> investors = new StorageList<>();
	private int previousLayerSize = 1;

	public @Payable @Entry(PayableContract.class) SimplePyramidDirectPay(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "You must invest at least " + MINIMUM_INVESTMENT);
		investors.add((PayableContract) caller());
	}

	public @Payable @Entry(PayableContract.class) void invest(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "You must invest at least " + MINIMUM_INVESTMENT);
		investors.add((PayableContract) caller());

		if (investors.size() == previousLayerSize * 4 - 1) {
			// pay out previous layer: note that currentLayer's size is even here
			investors.stream().skip(previousLayerSize - 1).limit(previousLayerSize).forEach(investor -> send(investor, MINIMUM_INVESTMENT));
			// spread remaining money among all participants
			BigInteger eachInvestorGets = balance().divide(BigInteger.valueOf(investors.size()));
			investors.stream().forEach(investor -> send(investor, eachInvestorGets));
			previousLayerSize *= 2;
		}
	}

	private void send(PayableContract investor, BigInteger amount) {
		investor.receive(amount);
	}
}