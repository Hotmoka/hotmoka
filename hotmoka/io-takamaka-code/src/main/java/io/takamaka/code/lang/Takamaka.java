package io.takamaka.code.lang;

import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * A class that acts as a global context for support methods of Takamaka code.
 */
public abstract class Takamaka {

	private Takamaka() {}

	/**
	 * Requires that the given condition holds.
	 * This is a synonym of {@link io.takamaka.code.lang.Takamaka#requireThat(boolean, String)}.
	 * 
	 * @param condition the condition that must hold
	 * @param message the message used in the exception raised if the condition does not hold
	 * @throws RequirementViolationException if the condition does not hold
	 */
	public static void require(boolean condition, String message) {
		if (!condition)
			throw new RequirementViolationException(message);
	}

	/**
	 * Requires that the given condition holds.
	 * This is a synonym of {@link io.takamaka.code.lang.Takamaka#require(boolean, String)}.
	 * 
	 * @param condition the condition that must hold
	 * @param message the message used in the exception raised if the condition does not hold
	 * @throws RequirementViolationException if the condition does not hold
	 */
	public static void requireThat(boolean condition, String message) {
		if (!condition)
			throw new RequirementViolationException(message);
	}

	/**
	 * Asserts that the given condition holds.
	 * 
	 * @param condition the condition that must hold
	 * @param message the message used in the exception raised if the condition does not hold
	 * @throws AssertionViolationException if the condition does not hold
	 */
	public static void assertThat(boolean condition, String message) {
		if (!condition)
			throw new AssertionViolationException(message);
	}

	/**
	 * Requires that the given condition holds.
	 * This is a synonym of {@link io.takamaka.code.lang.Takamaka#requireThat(boolean, Supplier)}.
	 * 
	 * @param condition the condition that must hold
	 * @param message the supplier of the message used in the exception raised if the condition does not hold
	 * @throws RequirementViolationException if the condition does not hold
	 */
	public static void require(boolean condition, Supplier<String> message) {
		if (!condition)
			throw new RequirementViolationException(message.get());
	}

	/**
	 * Requires that the given condition holds.
	 * This is a synonym of {@link io.takamaka.code.lang.Takamaka#require(boolean, Supplier)}.
	 * 
	 * @param condition the condition that must hold
	 * @param message the supplier of the message used in the exception raised if the condition does not hold
	 * @throws RequirementViolationException if the condition does not hold
	 */
	public static void requireThat(boolean condition, Supplier<String> message) {
		if (!condition)
			throw new RequirementViolationException(message.get());
	}

	/**
	 * Asserts that the given condition holds.
	 * 
	 * @param condition the condition that must hold
	 * @param message the supplier of the message used in the exception raised if the condition does not hold
	 * @throws AssertionViolationException if the condition does not hold
	 */
	public static void assertThat(boolean condition, Supplier<String> message) {
		if (!condition)
			throw new AssertionViolationException(message.get());
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
	public static <T> T withGas(BigInteger amount, Callable<T> what) throws Exception {
		// code provided by instrumentation as
		// return Runtime.withGas(amount, what);
		return null;
	}

	/**
	 * Takes note of the given event.
	 * 
	 * @param event the event
	 */
	public static void event(Event event) {
		// code provided by instrumentation as
		// Runtime.event(event);
	}

	public static long now() {
		// code provided by instrumentation as
		// return Runtime.now();
		return 0L;
	}
}