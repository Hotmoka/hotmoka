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
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.internal.gson.TransactionResponseJson;
import io.hotmoka.node.internal.values.StorageReferenceImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of a response for a successful transaction that calls a method
 * in the store of a node. The method has been called without problems and
 * without generating exceptions. The method returns {@code void}.
 */
@Immutable
public class VoidMethodCallTransactionSuccessfulResponseImpl extends MethodCallTransactionResponseImpl implements VoidMethodCallTransactionSuccessfulResponse {
	final static byte SELECTOR = 12;
	final static byte SELECTOR_NO_EVENTS = 16;
	final static byte SELECTOR_ONE_EVENT = 17;

	/**
	 * The events generated by this transaction.
	 */
	private final StorageReference[] events;

	/**
	 * Builds the transaction response.
	 * 
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public VoidMethodCallTransactionSuccessfulResponseImpl(Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		this(updates.toArray(Update[]::new), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, events.toArray(StorageReference[]::new), IllegalArgumentException::new);
	}

	/**
	 * Unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @param selector the selector
	 * @throws IOException if the response could not be unmarshalled
	 */
	public VoidMethodCallTransactionSuccessfulResponseImpl(UnmarshallingContext context, byte selector) throws IOException {
		this(
			context.readLengthAndArray(Updates::from, Update[]::new),
			context.readBigInteger(),
			context.readBigInteger(),
			context.readBigInteger(),
			unmarshalEvents(context, selector),
			IOException::new
		);
	}

	/**
	 * Creates a response from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public VoidMethodCallTransactionSuccessfulResponseImpl(TransactionResponseJson json) throws InconsistentJsonException {
		this(
			unmapUpdates(json),
			json.getGasConsumedForCPU(),
			json.getGasConsumedForRAM(),
			json.getGasConsumedForStorage(),
			unmapEvents(json),
			InconsistentJsonException::new
		);
	}

	/**
	 * Builds the transaction response.
	 * 
	 * @param <E> the type of the exception thrown if some argument is illegal
	 * @param updates the updates resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @param events the events resulting from the execution of the transaction
	 * @param onIllegalArgs the creator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> VoidMethodCallTransactionSuccessfulResponseImpl(Update[] updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, StorageReference[] events, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, IllegalArgumentException::new);

		this.events = Objects.requireNonNull(events, "events cannot be null", onIllegalArgs);
		for (var event: events)
			Objects.requireNonNull(event, "events cannot hold null elements", onIllegalArgs);
	}

	@Override
	public Stream<StorageReference> getEvents() {
		return Stream.of(events);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof VoidMethodCallTransactionSuccessfulResponse vmctsr && super.equals(other) && Arrays.equals(events, vmctsr.getEvents().toArray(StorageReference[]::new));
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(events);
	}

	@Override
	public String toString() {
        return super.toString() + "\n  events:\n" + getEvents().map(StorageReference::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		boolean optimized0 = events.length == 0;
		boolean optimized1 = events.length == 1;
		context.writeByte(optimized0 ? SELECTOR_NO_EVENTS : (optimized1 ? SELECTOR_ONE_EVENT : SELECTOR));
		super.into(context);

		if (!optimized0 && !optimized1)
			intoArrayWithoutSelector(events, context);

		if (optimized1)
			events[0].intoWithoutSelector(context);
	}

	private static StorageReference[] unmarshalEvents(UnmarshallingContext context, byte selector) throws IOException {
		if (selector == SELECTOR)
			return context.readLengthAndArray(StorageReferenceImpl::fromWithoutSelector, StorageReference[]::new);
		else if (selector == SELECTOR_NO_EVENTS)
			return NO_REFERENCES;
		else if (selector == SELECTOR_ONE_EVENT)
			return new StorageReference[] { StorageReferenceImpl.fromWithoutSelector(context) };
		else
			throw new IOException("Unexpected response selector: " + selector);
	}
}