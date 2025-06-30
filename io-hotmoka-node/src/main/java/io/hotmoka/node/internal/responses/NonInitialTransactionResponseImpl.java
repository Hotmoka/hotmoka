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

package io.hotmoka.node.internal.responses;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.api.responses.NonInitialTransactionResponse;
import io.hotmoka.node.api.updates.Update;

/**
 * A response for a non-initial transaction.
 */
@Immutable
public abstract class NonInitialTransactionResponseImpl extends TransactionResponseImpl implements NonInitialTransactionResponse {

	/**
	 * The updates resulting from the execution of the transaction.
	 */
	private final Update[] updates;

	/**
	 * The amount of gas consumed by the transaction for CPU execution.
	 */
	private final BigInteger gasConsumedForCPU;

	/**
	 * The amount of gas consumed by the transaction for RAM allocation.
	 */
	private final BigInteger gasConsumedForRAM;

	/**
	 * The amount of gas consumed by the transaction for storage consumption.
	 */
	private final BigInteger gasConsumedForStorage;

	/**
	 * Builds the transaction response.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @param onIllegalArgs the creator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	protected <E extends Exception> NonInitialTransactionResponseImpl(Update[] updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, ExceptionSupplierFromMessage<? extends E> onIllegalArgs) throws E {
		this.gasConsumedForCPU = Objects.requireNonNull(gasConsumedForCPU, "gasConsumedForCPU cannot be null", onIllegalArgs);
		if (gasConsumedForCPU.signum() < 0)
			throw onIllegalArgs.apply("gasConsumedForCPU cannot be negative");

		this.gasConsumedForRAM = Objects.requireNonNull(gasConsumedForRAM, "gasConsumedForRAM cannot be null", onIllegalArgs);
		if (gasConsumedForRAM.signum() < 0)
			throw onIllegalArgs.apply("gasConsumedForRAM cannot be negative");

		this.gasConsumedForStorage = Objects.requireNonNull(gasConsumedForStorage, "gasConsumedForStorage cannot be null", onIllegalArgs);
		if (gasConsumedForStorage.signum() < 0)
			throw onIllegalArgs.apply("gasConsumedForStorage cannot be negative");

		this.updates = Objects.requireNonNull(updates, "updates cannot be null", onIllegalArgs);
		for (var update: updates)
			Objects.requireNonNull(update, "updates cannot hold null elements", onIllegalArgs);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof NonInitialTransactionResponseImpl nitri) // optimization
			return Arrays.equals(updates, nitri.updates)
					&& gasConsumedForCPU.equals(nitri.gasConsumedForCPU)
					&& gasConsumedForRAM.equals(nitri.gasConsumedForRAM)
					&& gasConsumedForStorage.equals(nitri.gasConsumedForStorage);
		else
			return other instanceof NonInitialTransactionResponse nitr
					&& Arrays.equals(updates, nitr.getUpdates().toArray(Update[]::new))
					&& gasConsumedForCPU.equals(nitr.getGasConsumedForCPU())
					&& gasConsumedForRAM.equals(nitr.getGasConsumedForRAM())
					&& gasConsumedForStorage.equals(nitr.getGasConsumedForStorage());
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(updates) ^ gasConsumedForCPU.hashCode() ^ gasConsumedForRAM.hashCode() ^ gasConsumedForStorage.hashCode();
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n" + gasToString()
        	+ "  updates:\n" + getUpdates().map(Update::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	/**
	 * Yields a description of the gas consumption.
	 * 
	 * @return the description
	 */
	protected String gasToString() {
		return "  gas consumed for CPU execution: " + gasConsumedForCPU + "\n"
			+ "  gas consumed for RAM allocation: " + gasConsumedForRAM + "\n"
	        + "  gas consumed for storage consumption: " + gasConsumedForStorage + "\n";
	}

	@Override
	public final Stream<Update> getUpdates() {
		return Stream.of(updates);
	}

	@Override
	public final BigInteger getGasConsumedForCPU() {
		return gasConsumedForCPU;
	}

	@Override
	public final BigInteger getGasConsumedForRAM() {
		return gasConsumedForRAM;
	}

	@Override
	public final BigInteger getGasConsumedForStorage() {
		return gasConsumedForStorage;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeLengthAndArray(updates);
		context.writeBigInteger(gasConsumedForCPU);
		context.writeBigInteger(gasConsumedForRAM);
		context.writeBigInteger(gasConsumedForStorage);
	}
}