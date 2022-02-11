/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

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
     */
	public static <T> T withGas(BigInteger amount, Callable<T> what) {
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

	public static @View long now() {
		// code provided by instrumentation as
		// return Runtime.now();
		return 0L;
	}

	/**
	 * Determines if the execution was started by the node itself.
	 * This is always false if the node has no notion of commit.
	 * If the execution has been started by a user request, this will
	 * always be false.
	 * 
	 * @return true if and only if that condition occurs
	 */
	public static boolean isSystemCall() {
		// code provided by instrumentation as
		// return Runtime.isSystemCall();
		return false;
	}
}