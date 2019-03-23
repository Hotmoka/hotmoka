package takamaka.blockchain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public final class Gas {
	private static long gas;
	private static List<Long> oldGas = new ArrayList<>();

	static void init(long gas) {
		Gas.gas = gas;
		Gas.oldGas.clear();
	}

	public static long remaining() {
		return gas;
	}

	public static void charge(long amount) {
		if (amount <= 0L)
			throw new IllegalArgumentException("Gas can only decrease");

		gas -= amount;
		if (gas < 0L)
			throw new OutOfGasException(gas);
	}

	public static void charge(int amount) {
		charge((long) amount);
	}

	public static void charge(float amount) {
		charge((long) Math.ceil(amount));
	}

	public static <T> T withGas(long amount, Callable<T> what) throws Exception {
		charge(amount);
		oldGas.add(gas);
		gas = amount;

		try {
			return what.call();
		}
		finally {
			gas += oldGas.remove(oldGas.size() - 1);
		}
	}
}