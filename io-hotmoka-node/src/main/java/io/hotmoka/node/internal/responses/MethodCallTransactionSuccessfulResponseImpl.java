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
import io.hotmoka.beans.api.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.Updates;

/**
 * Implementation of a response for a successful transaction that calls a method
 * in the store of the node. The method has been called without problems and
 * without generating exceptions. The method does not return {@code void}.
 */
@Immutable
public class MethodCallTransactionSuccessfulResponseImpl extends MethodCallTransactionResponseImpl implements MethodCallTransactionSuccessfulResponse {
	final static byte SELECTOR = 9;
	final static byte SELECTOR_NO_EVENTS = 10;
	final static byte SELECTOR_ONE_EVENT = 11;

	/**
	 * The return value of the method.
	 */
	private final StorageValue result;

	/**
	 * The events generated by this transaction.
	 */
	private final StorageReference[] events;

	/**
	 * Builds the transaction response.
	 * 
	 * @param result the value returned by the method
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public MethodCallTransactionSuccessfulResponseImpl(StorageValue result, Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.result = Objects.requireNonNull(result);
		this.events = events.toArray(StorageReference[]::new);
		Stream.of(this.events).forEach(event -> Objects.requireNonNull(event, "events cannot hold null"));
	}

	@Override
	public final StorageValue getResult() {
		return result;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MethodCallTransactionSuccessfulResponse mctsr && super.equals(other)
			&& result.equals(mctsr.getResult()) && Arrays.equals(events, mctsr.getEvents().toArray(StorageReference[]::new));
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(events) ^ result.hashCode();
	}

	@Override
	public String toString() {
        return super.toString() + "\n"
        	+ "  returned value: " + result + "\n"
        	+ "  events:\n" + getEvents().map(StorageReference::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	@Override
	public Stream<StorageReference> getEvents() {
		return Stream.of(events);
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		boolean optimized = events.length == 0;
		boolean optimized1 = events.length == 1;
		context.writeByte(optimized ? SELECTOR_NO_EVENTS : (optimized1 ? SELECTOR_ONE_EVENT : SELECTOR));
		super.into(context);
		result.into(context);

		if (!optimized && !optimized1)
			intoArrayWithoutSelector(events, context);

		if (optimized1)
			events[0].intoWithoutSelector(context);
	}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @param selector the selector
	 * @return the response
	 * @throws IOException if the response could not be unmarshalled
	 */
	public static MethodCallTransactionSuccessfulResponseImpl from(UnmarshallingContext context, byte selector) throws IOException {
		Stream<Update> updates = Stream.of(context.readLengthAndArray(Updates::from, Update[]::new));
		var gasConsumedForCPU = context.readBigInteger();
		var gasConsumedForRAM = context.readBigInteger();
		var gasConsumedForStorage = context.readBigInteger();
		var result = StorageValues.from(context);
		Stream<StorageReference> events;

		if (selector == SELECTOR)
			events = Stream.of(context.readLengthAndArray(StorageValues::referenceWithoutSelectorFrom, StorageReference[]::new));
		else if (selector == SELECTOR_NO_EVENTS)
			events = Stream.empty();
		else if (selector == SELECTOR_ONE_EVENT)
			events = Stream.of(StorageValues.referenceWithoutSelectorFrom(context));
		else
			throw new IOException("Unexpected response selector: " + selector);

		return new MethodCallTransactionSuccessfulResponseImpl(result, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}
}