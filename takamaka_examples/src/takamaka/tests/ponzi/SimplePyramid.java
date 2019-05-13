package takamaka.tests.ponzi;

import static takamaka.lang.Takamaka.require;

import java.math.BigInteger;
import java.util.stream.Stream;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.PayableContract;
import takamaka.util.StorageList;
import takamaka.util.StorageMap;

public class SimplePyramid extends Contract {
	public final BigInteger MINIMUM_INVESTMENT = BigInteger.valueOf(1_000L);
	private final StorageList<PayableContract> oldLayers = new StorageList<>();
	private StorageList<PayableContract> previousLayer = new StorageList<>();
	private StorageList<PayableContract> currentLayer = new StorageList<>();
	private final StorageMap<PayableContract, BigInteger> balances = new StorageMap<>();
	private BigInteger pyramidBalance = BigInteger.ZERO;

	public @Payable @Entry(PayableContract.class) SimplePyramid(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "You must invest at least " + MINIMUM_INVESTMENT);
		previousLayer.add((PayableContract) caller());
		pyramidBalance = pyramidBalance.add(amount);
	}

	public @Payable @Entry(Payable.class) void invest(BigInteger amount) {
		require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "You must invest at least " + MINIMUM_INVESTMENT);
		pyramidBalance = pyramidBalance.add(amount);
		currentLayer.add((PayableContract) caller());

		if (currentLayer.size() == previousLayer.size() * 2) {
			// pay out previous layer and move it into oldLayers
			previousLayer.stream().forEach(investor -> {
				balances.update(investor, BigInteger.ZERO, MINIMUM_INVESTMENT::add);
				pyramidBalance = pyramidBalance.subtract(MINIMUM_INVESTMENT);
				oldLayers.add(investor);
			});

			// move current layer as future previous layer
			previousLayer = currentLayer;
			// reset current layer
			currentLayer = new StorageList<>();
			// spread remaining money among all participants
			BigInteger eachInvestorGets = pyramidBalance.divide(BigInteger.valueOf(oldLayers.size() + previousLayer.size()));
			Stream.concat(oldLayers.stream(), previousLayer.stream()).forEach(investor -> balances.update(investor, BigInteger.ZERO, eachInvestorGets::add));
			pyramidBalance = BigInteger.ZERO;
		}
	}

	public @Entry(PayableContract.class) void withdraw() {
		((PayableContract) caller()).receive(balances.get(caller()));
		balances.put((PayableContract) caller(), BigInteger.ZERO);
	}
}