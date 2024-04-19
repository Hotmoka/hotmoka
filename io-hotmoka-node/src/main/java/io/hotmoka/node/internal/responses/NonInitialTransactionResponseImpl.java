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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.marshalling.api.MarshallingContext;

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
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	protected NonInitialTransactionResponseImpl(Stream<Update> updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		this.gasConsumedForCPU = Objects.requireNonNull(gasConsumedForCPU, "gasConsumedForCPU cannot be null");
		this.gasConsumedForRAM = Objects.requireNonNull(gasConsumedForRAM, "gasConsumedForRAM cannot be null");
		this.gasConsumedForStorage = Objects.requireNonNull(gasConsumedForStorage, "gasConsumedForStorage cannot be null");
		this.updates = updates.toArray(Update[]::new);
		Stream.of(this.updates).forEach(update -> Objects.requireNonNull(update, "updates cannot hold null"));
	}

	@Override
	public boolean equals(Object other) {
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