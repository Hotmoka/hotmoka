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
import java.io.OutputStream;
import java.util.stream.Stream;

import io.hotmoka.crypto.Hex;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.node.api.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.node.api.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.GameteCreationTransactionResponse;
import io.hotmoka.node.api.responses.InitializationTransactionResponse;
import io.hotmoka.node.api.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.node.api.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.node.api.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.node.api.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.node.api.responses.NonVoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.internal.gson.TransactionResponseJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Shared implementation of the response of a transaction.
 */
public abstract class TransactionResponseImpl extends AbstractMarshallable implements TransactionResponse {

	/**
	 * Creates the request.
	 */
	protected TransactionResponseImpl() {}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the response
	 * @throws IOException if the response cannot be unmarshalled
	 */
	public static TransactionResponse from(UnmarshallingContext context) throws IOException {
		byte selector = context.readByte();

		switch (selector) {
		case GameteCreationTransactionResponseImpl.SELECTOR: return new GameteCreationTransactionResponseImpl(context);
		case JarStoreInitialTransactionResponseImpl.SELECTOR: return JarStoreInitialTransactionResponseImpl.from(context);
		case InitializationTransactionResponseImpl.SELECTOR: return InitializationTransactionResponseImpl.from(context);
		case JarStoreTransactionFailedResponseImpl.SELECTOR: return JarStoreTransactionFailedResponseImpl.from(context);
		case JarStoreTransactionSuccessfulResponseImpl.SELECTOR: return JarStoreTransactionSuccessfulResponseImpl.from(context);
		case ConstructorCallTransactionExceptionResponseImpl.SELECTOR: return ConstructorCallTransactionExceptionResponseImpl.from(context);
		case ConstructorCallTransactionFailedResponseImpl.SELECTOR: return ConstructorCallTransactionFailedResponseImpl.from(context);
		case ConstructorCallTransactionSuccessfulResponseImpl.SELECTOR:
		case ConstructorCallTransactionSuccessfulResponseImpl.SELECTOR_NO_EVENTS: return ConstructorCallTransactionSuccessfulResponseImpl.from(context, selector);
		case MethodCallTransactionExceptionResponseImpl.SELECTOR: return MethodCallTransactionExceptionResponseImpl.from(context);
		case MethodCallTransactionFailedResponseImpl.SELECTOR: return MethodCallTransactionFailedResponseImpl.from(context);
		case MethodCallTransactionSuccessfulResponseImpl.SELECTOR:
		case MethodCallTransactionSuccessfulResponseImpl.SELECTOR_NO_EVENTS:
		case MethodCallTransactionSuccessfulResponseImpl.SELECTOR_ONE_EVENT: return MethodCallTransactionSuccessfulResponseImpl.from(context, selector);
		case VoidMethodCallTransactionSuccessfulResponseImpl.SELECTOR:
		case VoidMethodCallTransactionSuccessfulResponseImpl.SELECTOR_NO_EVENTS: return VoidMethodCallTransactionSuccessfulResponseImpl.from(context, selector);
		default: throw new IOException("Unexpected response selector: " + selector);
		}
	}

	/**
	 * Factory method that extracts a response from its given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @return the response
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public static TransactionResponse from(TransactionResponseJson json) throws InconsistentJsonException {
		String type = json.getType();

		if (GameteCreationTransactionResponse.class.getSimpleName().equals(type))
			return new GameteCreationTransactionResponseImpl(json);
		else if (InitializationTransactionResponse.class.getSimpleName().equals(type))
			return TransactionResponses.initialization();
		else if (JarStoreInitialTransactionResponse.class.getSimpleName().equals(type))
			return TransactionResponses.jarStoreInitial(getBytesOfJar(json), Stream.of(unmapDependencies(json)), json.getVerificationToolVersion());
		else if (JarStoreTransactionFailedResponse.class.getSimpleName().equals(type))
			return TransactionResponses.jarStoreFailed(json.getClassNameOfCause(), json.getMessageOfCause(), Stream.of(unmapUpdates(json)), json.getGasConsumedForCPU(), json.getGasConsumedForRAM(), json.getGasConsumedForStorage(), json.getGasConsumedForPenalty());
		else if (JarStoreTransactionSuccessfulResponse.class.getSimpleName().equals(type))
			return TransactionResponses.jarStoreSuccessful(getBytesOfJar(json), Stream.of(unmapDependencies(json)), json.getVerificationToolVersion(), Stream.of(unmapUpdates(json)), json.getGasConsumedForCPU(), json.getGasConsumedForRAM(), json.getGasConsumedForStorage());
		else if (ConstructorCallTransactionExceptionResponse.class.getSimpleName().equals(type))
			return TransactionResponses.constructorCallException(json.getClassNameOfCause(), json.getMessageOfCause(), json.getWhere(), Stream.of(unmapUpdates(json)), Stream.of(unmapEvents(json)), json.getGasConsumedForCPU(), json.getGasConsumedForRAM(), json.getGasConsumedForStorage());
		else if (ConstructorCallTransactionFailedResponse.class.getSimpleName().equals(type))
			return TransactionResponses.constructorCallFailed(json.getClassNameOfCause(), json.getMessageOfCause(), json.getWhere(), Stream.of(unmapUpdates(json)), json.getGasConsumedForCPU(), json.getGasConsumedForRAM(), json.getGasConsumedForStorage(), json.getGasConsumedForPenalty());
		else if (ConstructorCallTransactionSuccessfulResponse.class.getSimpleName().equals(type))
			return TransactionResponses.constructorCallSuccessful(unmapIntoStorageReference(json.getNewObject()), Stream.of(unmapUpdates(json)), Stream.of(unmapEvents(json)), json.getGasConsumedForCPU(), json.getGasConsumedForRAM(), json.getGasConsumedForStorage());
		else if (MethodCallTransactionExceptionResponse.class.getSimpleName().equals(type))
			return TransactionResponses.methodCallException(json.getClassNameOfCause(), json.getMessageOfCause(), json.getWhere(), Stream.of(unmapUpdates(json)), Stream.of(unmapEvents(json)), json.getGasConsumedForCPU(), json.getGasConsumedForRAM(), json.getGasConsumedForStorage());
		else if (MethodCallTransactionFailedResponse.class.getSimpleName().equals(type))
			return TransactionResponses.methodCallFailed(json.getClassNameOfCause(), json.getMessageOfCause(), json.getWhere(), Stream.of(unmapUpdates(json)), json.getGasConsumedForCPU(), json.getGasConsumedForRAM(), json.getGasConsumedForStorage(), json.getGasConsumedForPenalty());
		else if (NonVoidMethodCallTransactionSuccessfulResponse.class.getSimpleName().equals(type))
			return TransactionResponses.methodCallSuccessful(json.getResult().unmap(), Stream.of(unmapUpdates(json)), Stream.of(unmapEvents(json)), json.getGasConsumedForCPU(), json.getGasConsumedForRAM(), json.getGasConsumedForStorage());
		else if (VoidMethodCallTransactionSuccessfulResponse.class.getSimpleName().equals(type))
			return TransactionResponses.voidMethodCallSuccessful(Stream.of(unmapUpdates(json)), Stream.of(unmapEvents(json)), json.getGasConsumedForCPU(), json.getGasConsumedForRAM(), json.getGasConsumedForStorage());
		else
			throw new InconsistentJsonException("Unexpected response type " + type);
	}

	protected static StorageReference unmapIntoStorageReference(StorageValues.Json json) throws InconsistentJsonException {
		if (json.unmap() instanceof StorageReference sr)
			return sr;
		else
			throw new InconsistentJsonException("Unexpected storage value");
	}

	protected static Update[] unmapUpdates(TransactionResponseJson json) throws InconsistentJsonException {
		var updatesAsArray = json.getUpdates().toArray(Updates.Json[]::new);
		var result = new Update[updatesAsArray.length];

		int pos = 0;
		for (var updateJson: updatesAsArray)
			result[pos++] = Objects.requireNonNull(updateJson, "updates cannot hold null elements", InconsistentJsonException::new).unmap();

		return result;
	}

	protected static TransactionReference[] unmapDependencies(TransactionResponseJson json) throws InconsistentJsonException {
		var dependenciesAsArray = json.getDependencies().toArray(TransactionReferences.Json[]::new);
		var result = new TransactionReference[dependenciesAsArray.length];

		int pos = 0;
		for (var dependencyJson: dependenciesAsArray)
			result[pos++] = Objects.requireNonNull(dependencyJson, "dependencies cannot hold null elements", InconsistentJsonException::new).unmap();

		return result;
	}

	protected static StorageReference[] unmapEvents(TransactionResponseJson json) throws InconsistentJsonException {
		var eventsAsArray = json.getEvents().toArray(StorageValues.Json[]::new);
		var result = new StorageReference[eventsAsArray.length];

		int pos = 0;
		for (var eventJson: eventsAsArray)
			result[pos++] = Objects.requireNonNull(eventJson, "events cannot hold null elements", InconsistentJsonException::new)
				.unmap()
				.asReference(v -> new InconsistentJsonException("events should hold storage references, not a " + v.getClass().getSimpleName()));

		return result;
	}

	private static byte[] getBytesOfJar(TransactionResponseJson json) throws InconsistentJsonException {
		return Hex.fromHexString(
			Objects.requireNonNull(json.getInstrumentedJar(), "instrumentedJar cannot be null", InconsistentJsonException::new),
			s -> new InconsistentJsonException("The bytes of the instrumented jar cannot be reconstructed: " + s)
		);
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return NodeMarshallingContexts.of(os);
	}
}