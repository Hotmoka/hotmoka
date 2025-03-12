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

package io.hotmoka.node.internal.requests;

import java.io.IOException;
import java.io.OutputStream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.internal.gson.TransactionRequestJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Shared implementation of a request of a transaction.
 * 
 * @param <R> the type of the response expected for this request
 */
@Immutable
public abstract class TransactionRequestImpl<R extends TransactionResponse> extends AbstractMarshallable implements TransactionRequest<R> {

	/**
	 * Creates the request.
	 */
	protected TransactionRequestImpl() {}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the request cannot be unmarshalled
	 */
	public static TransactionRequest<?> from(UnmarshallingContext context) throws IOException {
		byte selector = context.readByte();
		switch (selector) {
		case ConstructorCallTransactionRequestImpl.SELECTOR: return ConstructorCallTransactionRequestImpl.from(context);
		case InitializationTransactionRequestImpl.SELECTOR: return InitializationTransactionRequestImpl.from(context);
		case InstanceMethodCallTransactionRequestImpl.SELECTOR:
		case InstanceMethodCallTransactionRequestImpl.SELECTOR_TRANSFER_INT:
		case InstanceMethodCallTransactionRequestImpl.SELECTOR_TRANSFER_LONG:
		case InstanceMethodCallTransactionRequestImpl.SELECTOR_TRANSFER_BIG_INTEGER:
			return InstanceMethodCallTransactionRequestImpl.from(context, selector);
		case JarStoreInitialTransactionRequestImpl.SELECTOR: return JarStoreInitialTransactionRequestImpl.from(context);
		case JarStoreTransactionRequestImpl.SELECTOR: return JarStoreTransactionRequestImpl.from(context);
		case GameteCreationTransactionRequestImpl.SELECTOR: return GameteCreationTransactionRequestImpl.from(context);
		case StaticMethodCallTransactionRequestImpl.SELECTOR: return StaticMethodCallTransactionRequestImpl.from(context);
		case InstanceSystemMethodCallTransactionRequestImpl.SELECTOR: return InstanceSystemMethodCallTransactionRequestImpl.from(context);
		default: throw new IOException("Unexpected request selector: " + selector);
		}
	}

	/**
	 * Yields a transaction request from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @return the resulting transaction request
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public static TransactionRequest<?> from(TransactionRequestJson json) throws InconsistentJsonException {
		String type = Objects.requireNonNull(json.getType(), "type cannot be null", InconsistentJsonException::new);

		if (JarStoreTransactionRequest.class.getSimpleName().equals(type))
			return new JarStoreTransactionRequestImpl(json);
		else if (GameteCreationTransactionRequest.class.getSimpleName().equals(type))
			return new GameteCreationTransactionRequestImpl(json);
		else if (InitializationTransactionRequest.class.getSimpleName().equals(type))
			return new InitializationTransactionRequestImpl(json);
		else if (JarStoreInitialTransactionRequest.class.getSimpleName().equals(type))
			return new JarStoreInitialTransactionRequestImpl(json);
		else
			throw new InconsistentJsonException("Unexpected request type " + type);
	}

	private static StorageValue[] convertedActuals(TransactionRequestJson json) throws InconsistentJsonException {
		StorageValues.Json[] actuals = json.getActuals().toArray(StorageValues.Json[]::new);
		var result = new StorageValue[actuals.length];
		for (int pos = 0; pos < result.length; pos++) {
			StorageValues.Json actual = actuals[pos];
			if (actual == null)
				throw new InconsistentJsonException("actuals cannot hold null elements");

			result[pos] = actual.unmap();
		}

		return result;
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return NodeMarshallingContexts.of(os);
	}
}