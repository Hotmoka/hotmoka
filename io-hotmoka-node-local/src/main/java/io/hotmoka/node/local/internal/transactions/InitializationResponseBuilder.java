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

package io.hotmoka.node.local.internal.transactions;

import java.io.IOException;
import java.util.NoSuchElementException;

import io.hotmoka.beans.TransactionResponses;
import io.hotmoka.beans.api.requests.InitializationTransactionRequest;
import io.hotmoka.beans.api.responses.InitializationTransactionResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.local.AbstractInitialResponseBuilder;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.UnsupportedVerificationVersionException;
import io.hotmoka.node.local.internal.NodeInternal;

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
	 */
	public InitializationResponseBuilder(TransactionReference reference, InitializationTransactionRequest request, NodeInternal node) throws TransactionRejectedException {
		super(reference, request, node);
	}

	@Override
	public InitializationTransactionResponse getResponse() throws TransactionRejectedException {
		return new ResponseCreator() {

			@Override
			protected InitializationTransactionResponse body() {
				return TransactionResponses.initialization();	
			}
		}
		.create();
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws ClassNotFoundException, UnsupportedVerificationVersionException, IOException, NoSuchElementException, NodeException {
		return node.getCaches().getClassLoader(request.getClasspath()); // currently not used for this transaction
	}
}