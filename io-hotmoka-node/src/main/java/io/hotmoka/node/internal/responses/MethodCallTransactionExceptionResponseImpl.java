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
import io.hotmoka.beans.api.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.Updates;

/**
 * Implementation of a response for a successful transaction that calls a method in the store of the node.
 * The method is annotated as {@code io.takamaka.code.lang.ThrowsExceptions}.
 * It has been called without problems but it threw an exception.
 */
@Immutable
public class MethodCallTransactionExceptionResponseImpl extends MethodCallTransactionResponseImpl implements MethodCallTransactionExceptionResponse {
	public final static byte SELECTOR = 7;

	/**
	 * The events generated by this transaction.
	 */
	private final StorageReference[] events;

	/**
	 * The fully-qualified class name of the cause exception.
	 */
	private final String classNameOfCause;

	/**
	 * The message of the cause exception.
	 */
	private final String messageOfCause;

	/**
	 * The program point where the cause exception occurred.
	 */
	private final String where;

	/**
	 * Builds the transaction response.
	 * 
	 * @param classNameOfCause the fully-qualified class name of the cause exception
	 * @param messageOfCause of the message of the cause exception; this might be {@code null}
	 * @param where the program point where the cause exception occurred; this might be {@code null}
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public MethodCallTransactionExceptionResponseImpl(String classNameOfCause, String messageOfCause, String where, Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.classNameOfCause = Objects.requireNonNull(classNameOfCause, "classNameOfCause cannot be null");
		this.events = events.toArray(StorageReference[]::new);
		Stream.of(this.events).forEach(event -> Objects.requireNonNull(event, "events cannot hold null"));
		this.messageOfCause = messageOfCause == null ? "" : messageOfCause;
		this.where = where == null ? "" : where;
	}

	@Override
	public Stream<StorageReference> getEvents() {
		return Stream.of(events);
	}

	@Override
	public final String getClassNameOfCause() {
		return classNameOfCause;
	}

	@Override
	public final String getMessageOfCause() {
		return messageOfCause;
	}

	@Override
	public final String getWhere() {
		return where;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MethodCallTransactionExceptionResponse mcter && super.equals(other)
			&& Arrays.equals(events, mcter.getEvents().toArray(StorageReference[]::new))
			&& classNameOfCause.equals(mcter.getClassNameOfCause())
			&& messageOfCause.equals(mcter.getMessageOfCause())
			&& where.equals(mcter.getWhere());
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(events) ^ classNameOfCause.hashCode() ^ messageOfCause.hashCode() ^ where.hashCode();
	}

	@Override
	public String toString() {
		if (messageOfCause.isEmpty())
			return super.toString() + "\n  throws: " + classNameOfCause + "\n  events:\n" + getEvents().map(StorageReference::toString).collect(Collectors.joining("\n    ", "    ", ""));
		else
			return super.toString() + "\n  throws: " + classNameOfCause + ":" + messageOfCause + "\n  events:\n" + getEvents().map(StorageReference::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		super.into(context);
		intoArrayWithoutSelector(events, context);
		context.writeStringUnshared(classNameOfCause);
		context.writeStringUnshared(messageOfCause);
		context.writeStringUnshared(where);
	}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @return the response
	 * @throws IOException if the response could not be unmarshalled
	 */
	public static MethodCallTransactionExceptionResponseImpl from(UnmarshallingContext context) throws IOException {
		Stream<Update> updates = Stream.of(context.readLengthAndArray(Updates::from, Update[]::new));
		var gasConsumedForCPU = context.readBigInteger();
		var gasConsumedForRAM = context.readBigInteger();
		var gasConsumedForStorage = context.readBigInteger();
		Stream<StorageReference> events = Stream.of(context.readLengthAndArray(StorageValues::referenceWithoutSelectorFrom, StorageReference[]::new));
		var classNameOfCause = context.readStringUnshared();
		var messageOfCause = context.readStringUnshared();
		var where = context.readStringUnshared();
		return new MethodCallTransactionExceptionResponseImpl(classNameOfCause, messageOfCause, where, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}
}