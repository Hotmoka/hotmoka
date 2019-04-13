package takamaka.blockchain;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public final class Gas {
	private static BigInteger gas;
	private static List<BigInteger> oldGas = new ArrayList<>();

	static void init(BigInteger gas) {
		Gas.gas = gas;
		Gas.oldGas.clear();
	}

	public static BigInteger remaining() {
		return gas;
	}

	public static void charge(BigInteger amount) {
		if (amount.signum() <= 0)
			throw new IllegalArgumentException("Gas can only decrease");

		gas = gas.subtract(amount);
		if (gas.signum() < 0)
			throw new OutOfGasError(gas);
	}

	public static void charge(long amount) {
		charge(BigInteger.valueOf(amount));
	}

	public static void charge(int amount) {
		charge(BigInteger.valueOf(amount));
	}

	public static <T> T withGas(BigInteger amount, Callable<T> what) throws Exception {
		charge(amount);
		oldGas.add(gas);
		gas = amount;

		try {
			return what.call();
		}
		finally {
			gas = gas.add(oldGas.remove(oldGas.size() - 1));
		}
	}
}