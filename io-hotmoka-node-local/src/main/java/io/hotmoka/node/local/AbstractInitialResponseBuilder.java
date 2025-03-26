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
import io.hotmoka.node.api.requests.InitialTransactionRequest;
import io.hotmoka.node.api.responses.InitialTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.internal.builders.ExecutionEnvironment;
import io.hotmoka.node.local.internal.builders.InitialResponseBuilderImpl;

/**
 * Partial implementation of the creator of the response for an initial transaction. Initial transactions do not consume gas.
 * 
 * @param <Request> the type of the request of the transaction
 * @param <Response> the type of the response of the transaction
 */
public abstract class AbstractInitialResponseBuilder<Request extends InitialTransactionRequest<Response>, Response extends InitialTransactionResponse>
		extends InitialResponseBuilderImpl<Request, Response> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param environment the execution environment where the response is built
	 */
	protected AbstractInitialResponseBuilder(TransactionReference reference, Request request, ExecutionEnvironment environment) {
		super(reference, request, environment);
	}

	/**
	 * The creator of the response from the request.
	 */
	protected abstract class ResponseCreator extends InitialResponseBuilderImpl<Request, Response>.ResponseCreator {

		/**
		 * Creates the response from the request.
		 * 
		 * @throws TransactionRejectedException if the request is rejected
		 * @throws StoreException if the store of the node misbehaves
		 */
		protected ResponseCreator() throws TransactionRejectedException, StoreException {}

		/**
		 * Checks if the request should be rejected, even before trying to execute it.
		 * 
		 * @throws TransactionRejectedException if the request should be rejected
		 */
		protected void checkConsistency() throws TransactionRejectedException {
			try {
				if (environment.getManifest().isPresent())
					throw new TransactionRejectedException("Cannot run an initial transaction request in an already initialized node", consensus);
			}
			catch (StoreException e) {
				new RuntimeException(e);
			}
		}
	}
}