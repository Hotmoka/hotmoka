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

package io.hotmoka.node.local;

import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.NonInitialTransactionRequest;
import io.hotmoka.node.api.responses.NonInitialTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.internal.builders.ExecutionEnvironment;
import io.hotmoka.node.local.internal.builders.NonInitialResponseBuilderImpl;

/**
 * Partial implementation of the creator of the response for a non-initial transaction.
 * Non-initial transactions consume gas, have a payer, a nonce, a chain identifier and are signed.
 * The constructor of this class checks the validity of all these elements.
 * 
 * @param <Request> the type of the request of the transaction
 * @param <Response> the type of the response of the transaction
 */
public abstract class AbstractNonInitialResponseBuilder<Request extends NonInitialTransactionRequest<Response>, Response extends NonInitialTransactionResponse> extends NonInitialResponseBuilderImpl<Request, Response> {

	/**
	 * Creates a the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param environment the execution environment where the response is built
	 */
	protected AbstractNonInitialResponseBuilder(TransactionReference reference, Request request, ExecutionEnvironment environment) {
		super(reference, request, environment);
	}

	/**
	 * The creator of the response from the request.
	 */
	protected abstract class ResponseCreator extends NonInitialResponseBuilderImpl<Request, Response>.ResponseCreator {

		/**
		 * Creates the response from the request.
		 * 
		 * @throws TransactionRejectedException if the request is rejected
		 * @throws StoreException if the store of the node misbehaves
		 */
		protected ResponseCreator() throws TransactionRejectedException, StoreException {}
	}
}