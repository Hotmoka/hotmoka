package takamaka.blockchain;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import takamaka.lang.OutOfGasError;
import takamaka.lang.WhiteListed;

/**
 * This class keeps track of the gas available for the transaction
 * currently being executed.
 */
public final class Gas {

	/**
	 * The remaining mount of gas, not yet consumed.
	 */
	private static BigInteger gas;

	/**
	 * A stack of available gas. When a sub-computation is started
	 * with a subset of the available gas, the latter is taken away from
	 * the current available gas and pushed on top of this stack.
	 */
	private final static LinkedList<BigInteger> oldGas = new LinkedList<>();

	/**
	 * Initializes this class with the given amount of gas.
	 * 
	 * @param gas the available gas
	 */
	static void init(BigInteger gas) {
		Gas.gas = gas;
		Gas.oldGas.clear();
	}

	/**
	 * Yields the amount of gas still available to the
	 * currently executing transaction.
	 * 
	 * @return the remaining gas
	 */
	static BigInteger remaining() {
		return gas;
	}

	/**
	 * Decreases the available gas by the given amount.
	 * 
	 * @param amount the amount of gas to consume
	 */
	public static void charge(BigInteger amount) {
		if (amount.signum() <= 0)
			throw new IllegalArgumentException("Gas can only decrease");

		if (gas.compareTo(amount) < 0)
			// we report how much gas is missing
			throw new OutOfGasError(amount.subtract(gas));

		gas = gas.subtract(amount);
	}

	/**
	 * Decreases the available gas by the given amount.
	 * 
	 * @param amount the amount of gas to consume
	 */
	static void charge(long amount) {
		charge(BigInteger.valueOf(amount));
	}

	/**
	 * Decreases the available gas by the given amount.
	 * 
	 * @param amount the amount of gas to consume
	 */
	static void charge(int amount) {
		charge(BigInteger.valueOf(amount));
	}

	/**
	 * Runs a given piece of code with a subset of the available gas.
	 * It first charges the given amount of gas. Then runs the code
	 * with the charged gas only. At its end, the remaining gas is added
	 * to the available gas to continue the computation.
	 * 
	 * @param amount the amount of gas provided to the code
	 * @param what the code to run
	 * @return the result of the execution of the code
	 * @throws OutOfGasError if there is not enough gas
	 * @throws Exception if the code runs into this exception
	 */
	@WhiteListed
	public static <T> T withGas(BigInteger amount, Callable<T> what) throws Exception {
		charge(amount);
		oldGas.addFirst(gas);
		gas = amount;

		try {
			return what.call();
		}
		finally {
			gas = gas.add(oldGas.removeFirst());
		}
	}
}