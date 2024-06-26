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
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;

/**
 * Implementation of a response for a successful transaction that calls a constructor to instantiate an object in the store
 * of the node. The constructor has been called without problems and without generating exceptions.
 */
@Immutable
public class ConstructorCallTransactionSuccessfulResponseImpl extends CodeExecutionTransactionResponseImpl implements ConstructorCallTransactionSuccessfulResponse {
	final static byte SELECTOR = 6;
	final static byte SELECTOR_NO_EVENTS = 13;

	/**
	 * The events generated by this transaction.
	 */
	private final StorageReference[] events;

	/**
	 * The object that has been created by the constructor call.
	 */
	private final StorageReference newObject;

	/**
	 * Builds the transaction response.
	 * 
	 * @param newObject the object that has been successfully created
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public ConstructorCallTransactionSuccessfulResponseImpl(StorageReference newObject, Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.newObject = Objects.requireNonNull(newObject, "newObject cannot be null");
		this.events = events.toArray(StorageReference[]::new);
		Stream.of(this.events).forEach(event -> Objects.requireNonNull(event, "events cannot hold null"));
	}

	@Override
	public String toString() {
        return super.toString() + "\n"
       		+ "  new object: " + newObject + "\n"
        	+ "  events:\n" + getEvents().map(StorageReference::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	@Override
	public final Stream<StorageReference> getEvents() {
		return Stream.of(events);
	}

	/**
	 * Yields the reference to the object that has been created.
	 * 
	 * @return the reference
	 */
	public final StorageReference getNewObject() {
		return newObject;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ConstructorCallTransactionSuccessfulResponse cctsr && super.equals(other)
			&& Arrays.equals(events, cctsr.getEvents().toArray(StorageReference[]::new)) && newObject.equals(cctsr.getNewObject());
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(events) ^ newObject.hashCode();
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(events.length == 0 ? SELECTOR_NO_EVENTS : SELECTOR);
		super.into(context);
		if (events.length > 0)
			intoArrayWithoutSelector(events, context);
		newObject.intoWithoutSelector(context);
	}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @param selector the selector
	 * @return the response
	 * @throws IOException if the response cannot be unmarshalled
	 */
	public static ConstructorCallTransactionSuccessfulResponseImpl from(UnmarshallingContext context, byte selector) throws IOException {
		Stream<Update> updates = Stream.of(context.readLengthAndArray(Updates::from, Update[]::new));
		var gasConsumedForCPU = context.readBigInteger();
		var gasConsumedForRAM = context.readBigInteger();
		var gasConsumedForStorage = context.readBigInteger();
		Stream<StorageReference> events;
		if (selector == SELECTOR)
			events = Stream.of(context.readLengthAndArray(StorageValues::referenceWithoutSelectorFrom, StorageReference[]::new));
		else if (selector == SELECTOR_NO_EVENTS)
			events = Stream.empty();
		else
			throw new IOException("Unexpected response selector: " + selector);

		var newObject = StorageValues.referenceWithoutSelectorFrom(context);
		return new ConstructorCallTransactionSuccessfulResponseImpl(newObject, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}
}