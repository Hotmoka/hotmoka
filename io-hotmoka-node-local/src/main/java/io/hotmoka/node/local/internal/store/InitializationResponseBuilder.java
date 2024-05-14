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

package io.hotmoka.node.local.internal.store;

import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.responses.InitializationTransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.AbstractInitialResponseBuilder;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.StoreException;

/**
 * The creator of a response for a transaction that initializes a node.
 */
public class InitializationResponseBuilder extends AbstractInitialResponseBuilder<InitializationTransactionRequest, InitializationTransactionResponse> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 * @throws StoreException 
	 */
	public InitializationResponseBuilder(TransactionReference reference, InitializationTransactionRequest request, ExecutionEnvironment environment) throws TransactionRejectedException, StoreException {
		super(reference, request, environment);
	}

	@Override
	public InitializationTransactionResponse getResponse() throws StoreException {
		return new ResponseCreator() {

			@Override
			protected InitializationTransactionResponse body() {
				return TransactionResponses.initialization();	
			}
		}
		.create();
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws StoreException {
		return environment.getClassLoader(request.getClasspath(), consensus); // currently not used for this transaction
	}
}