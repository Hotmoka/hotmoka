package takamaka.lang;

import java.math.BigInteger;
import java.util.concurrent.Callable;

import takamaka.blockchain.AbstractBlockchain;

/**
 * A class that acts as a global context for statements added
 * to the Takamaka language.
 */
public abstract class Takamaka {

	/**
	 * The blockchain used for the current transaction.
	 */
	private static AbstractBlockchain blockchain;

	/**
	 * The counter for the next storage object created during the current transaction.
	 */
	private static BigInteger nextProgressive;

	private Takamaka() {}

	/**
	 * Resets static data at the beginning of a transaction.
	 * 
	 * @param blockchain the blockchain used for the new transaction
	 */
	public static void init(AbstractBlockchain blockchain) {
		Takamaka.blockchain = blockchain;
		Takamaka.nextProgressive = BigInteger.ZERO;
	}

	/**
	 * Takes note of the given event. This method can only be called during
	 * a transaction.
	 * 
	 * @param event the event
	 */
	@WhiteListed
	public static void event(Event event) {
		blockchain.event(event);
	}

	/**
	 * Requires that the given condition holds.
	 * This is a synonym of {@link takamaka.lang.Takamaka#requireThat(boolean, String)}.
	 * 
	 * @param condition the condition that must hold
	 * @param message the message used in the exception raised if the
	 *                condition does not hold
	 * @throws RequirementViolationException if the condition does not hold
	 */
	@WhiteListed
	public static void require(boolean condition, String message) {
		if (!condition)
			throw new RequirementViolationException(message);
	}

	/**
	 * Requires that the given condition holds.
	 * This is a synonym of {@link takamaka.lang.Takamaka#require(boolean, String)}.
	 * 
	 * @param condition the condition that must hold
	 * @param message the message used in the exception raised if the
	 *                condition does not hold
	 * @throws RequirementViolationException if the condition does not hold
	 */
	@WhiteListed
	public static void requireThat(boolean condition, String message) {
		if (!condition)
			throw new RequirementViolationException(message);
	}

	/**
	 * Asserts that the given condition holds.
	 * 
	 * @param condition the condition that must hold
	 * @param message the message used in the exception raised if the
	 *                condition does not hold
	 * @throws AssertionViolationException if the condition does not hold
	 */
	@WhiteListed
	public static void assertThat(boolean condition, String message) {
		if (!condition)
			throw new AssertionViolationException(message);
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
		return blockchain.withGas(amount, what);
	}

	/**
	 * Yields the blockchain used by the current transaction.
	 * This method can only be called during a transaction.
	 * 
	 * @return the blockchain
	 */
	static AbstractBlockchain getBlockchain() {
		return blockchain;
	}

	/**
	 * Yields the next identifier that can be used for a new storage object
	 * created during the execution of the current transaction. This identifier is unique
	 * inside the transaction. This method will return distinct identifiers at each call.
	 * 
	 * @return the identifier
	 */
	static BigInteger generateNextProgressive() {
		BigInteger result = nextProgressive;
		nextProgressive = nextProgressive.add(BigInteger.ONE);
		return result;
	}
}