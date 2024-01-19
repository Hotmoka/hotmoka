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

package io.hotmoka.beans.internal.requests;

import java.io.IOException;
import java.io.OutputStream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.marshalling.BeanMarshallingContext;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceSystemMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

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
	public static TransactionRequestImpl<?> from(UnmarshallingContext context) throws IOException {
		byte selector = context.readByte();
		switch (selector) {
		case ConstructorCallTransactionRequest.SELECTOR: return ConstructorCallTransactionRequest.from(context);
		case InitializationTransactionRequest.SELECTOR: return InitializationTransactionRequest.from(context);
		case InstanceMethodCallTransactionRequest.SELECTOR:
		case InstanceMethodCallTransactionRequest.SELECTOR_TRANSFER_INT:
		case InstanceMethodCallTransactionRequest.SELECTOR_TRANSFER_LONG:
		case InstanceMethodCallTransactionRequest.SELECTOR_TRANSFER_BIG_INTEGER:
			return InstanceMethodCallTransactionRequest.from(context, selector);
		case JarStoreInitialTransactionRequest.SELECTOR: return JarStoreInitialTransactionRequest.from(context);
		case JarStoreTransactionRequest.SELECTOR: return JarStoreTransactionRequest.from(context);
		case GameteCreationTransactionRequest.SELECTOR: return GameteCreationTransactionRequest.from(context);
		case StaticMethodCallTransactionRequest.SELECTOR: return StaticMethodCallTransactionRequest.from(context);
		case InstanceSystemMethodCallTransactionRequest.SELECTOR: return InstanceSystemMethodCallTransactionRequest.from(context);
		default: throw new IOException("Unexpected request selector: " + selector);
		}
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return new BeanMarshallingContext(os);
	}
}