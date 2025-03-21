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
import io.hotmoka.node.api.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.internal.gson.TransactionResponseJson;
import io.hotmoka.node.internal.values.StorageReferenceImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of a response for a successful transaction that calls a constructor to instantiate an object in the store
 * of the node. The constructor is annotated as {@code io.takamaka.code.lang.ThrowsExceptions}.
 * It has been called without problems but it threw an exception.
 */
@Immutable
public class ConstructorCallTransactionExceptionResponseImpl extends CodeExecutionTransactionResponseImpl implements ConstructorCallTransactionExceptionResponse {
	final static byte SELECTOR = 4;

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
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 * @param classNameOfCause the fully-qualified class name of the cause exception
	 * @param messageOfCause of the message of the cause exception; this might be {@code null}
	 * @param where the program point where the cause exception occurred; this might be {@code null}
	 */
	public ConstructorCallTransactionExceptionResponseImpl(Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, String classNameOfCause, String messageOfCause, String where) {
		this(updates.toArray(Update[]::new), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, events.toArray(StorageReference[]::new), classNameOfCause, messageOfCause, where, IllegalArgumentException::new);
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
	 * @param classNameOfCause the fully-qualified class name of the cause exception
	 * @param messageOfCause of the message of the cause exception; this might be {@code null}
	 * @param where the program point where the cause exception occurred; this might be {@code null}
	 * @param onIllegalArgs the creator of the exception thrown if some argument is illegal
	 * @throws E if some argument is illegal
	 */
	private <E extends Exception> ConstructorCallTransactionExceptionResponseImpl(Update[] updates, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage, StorageReference[] events, String classNameOfCause, String messageOfCause, String where, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
		super(updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, onIllegalArgs);

		this.events = Objects.requireNonNull(events, "events cannot be null", onIllegalArgs);
		for (var event: events)
			Objects.requireNonNull(event, "events cannot hold null elements", onIllegalArgs);

		this.classNameOfCause = Objects.requireNonNull(classNameOfCause, "classNameOfCause cannot be null", onIllegalArgs);
		this.messageOfCause = messageOfCause == null ? "" : messageOfCause;
		this.where = where == null ? "" : where;
	}

	/**
	 * Unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @throws IOException if the response could not be unmarshalled
	 */
	public ConstructorCallTransactionExceptionResponseImpl(UnmarshallingContext context) throws IOException {
		this(
			context.readLengthAndArray(Updates::from, Update[]::new),
			context.readBigInteger(),
			context.readBigInteger(),
			context.readBigInteger(),
			context.readLengthAndArray(StorageReferenceImpl::fromWithoutSelector, StorageReference[]::new),
			context.readStringUnshared(),
			context.readStringUnshared(),
			context.readStringUnshared(),
			IOException::new
		);
	}

	/**
	 * Creates a response from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public ConstructorCallTransactionExceptionResponseImpl(TransactionResponseJson json) throws InconsistentJsonException {
		this(
			unmapUpdates(json),
			json.getGasConsumedForCPU(),
			json.getGasConsumedForRAM(),
			json.getGasConsumedForStorage(),
			unmapEvents(json),
			json.getClassNameOfCause(),
			json.getMessageOfCause(),
			json.getWhere(),
			InconsistentJsonException::new
		);
	}

	@Override
	public final Stream<StorageReference> getEvents() {
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
		return other instanceof ConstructorCallTransactionExceptionResponse ccter && super.equals(other)
			&& Arrays.equals(events, ccter.getEvents().toArray(StorageReference[]::new))
			&& classNameOfCause.equals(ccter.getClassNameOfCause())
			&& messageOfCause.equals(ccter.getMessageOfCause()) && where.equals(ccter.getWhere());
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(events) ^ classNameOfCause.hashCode()
			^ messageOfCause.hashCode() ^ where.hashCode();
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
}