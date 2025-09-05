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

package io.hotmoka.node.internal.transactions;

import java.io.IOException;
import java.io.OutputStream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.Transaction;
import io.hotmoka.node.internal.requests.TransactionRequestImpl;
import io.hotmoka.node.internal.responses.TransactionResponseImpl;

/**
 * Implementation of a Hotmoka transaction.
 * 
 * @param <R> the type of the response of the transaction
 */
@Immutable
public final class TransactionImpl extends AbstractMarshallable implements Transaction {

	/**
	 * The request of the transaction.
	 */
	private final TransactionRequest<?> request;

	/**
	 * The response of the transaction.
	 */
	private final TransactionResponse response;

	/**
	 * Creates the transaction.
	 * 
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 */
	public TransactionImpl(TransactionRequest<?> request, TransactionResponse response) {
		this.request = request;
		this.response = response;
	}

	/**
	 * Unmarshals a transaction from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @throws IOException if the transaction cannot be unmarshalled
	 */
	public TransactionImpl(UnmarshallingContext context) throws IOException {
		this.response = TransactionResponseImpl.from(context);
		this.request = TransactionRequestImpl.from(context);
	}

	@Override
	public TransactionRequest<?> getRequest() {
		return request;
	}

	@Override
	public TransactionResponse getResponse() {
		return response;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		response.into(context);
		request.into(context);
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) {
		return NodeMarshallingContexts.of(os);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Transaction otherAsTransaction && request.equals(otherAsTransaction.getRequest()) && response.equals(otherAsTransaction.getResponse());
	}

	@Override
	public int hashCode() {
		return request.hashCode() ^ response.hashCode();
	}

	@Override
	public String toString() {
		return "request: " + request + "\nresponse: " + response;
	}
}