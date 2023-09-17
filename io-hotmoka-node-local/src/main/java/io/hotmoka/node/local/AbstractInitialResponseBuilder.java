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

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InitialTransactionRequest;
import io.hotmoka.beans.responses.InitialTransactionResponse;
import io.hotmoka.node.local.internal.InitialResponseBuilderImpl;
import io.hotmoka.node.local.internal.NodeInternal;

/**
 * Partial implementation of the creator of the response for an initial transaction. Initial transactions do not consume gas.
 */
public abstract class AbstractInitialResponseBuilder
			<Request extends InitialTransactionRequest<Response>, Response extends InitialTransactionResponse>
		extends InitialResponseBuilderImpl<Request, Response> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is creating the response
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	protected AbstractInitialResponseBuilder(TransactionReference reference, Request request, NodeInternal node) throws TransactionRejectedException {
		super(reference, request, node);
	}

	protected abstract class ResponseCreator extends InitialResponseBuilderImpl<Request, Response>.ResponseCreator {

		protected ResponseCreator() throws TransactionRejectedException {
		}
	}
}