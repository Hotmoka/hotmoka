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

package io.hotmoka.node.local.api;

import java.util.NoSuchElementException;

import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;

/**
 * The creator of a response from a request. It executes a transaction from the request and builds the corresponding response.
 * 
 * @param <Request> the type of the request of the transaction
 * @param <Response> the type of the response of the transaction
 */
public interface ResponseBuilder<Request extends TransactionRequest<Response>, Response extends TransactionResponse> {

	/**
	 * Yield the request for which this builder was created.
	 * 
	 * @return the request
	 */
	Request getRequest();

	/**
	 * Yields the response corresponding to the request for which
	 * this builder was created.
	 * 
	 * @return the response
	 * @throws TransactionRejectedException if the response could not be created
	 */
	Response getResponse() throws TransactionRejectedException;
	
	/**
	 * Replaces all reverified responses into the store of the node for which the response is built.
	 */
	void replaceReverifiedResponses() throws NoSuchElementException, UnknownReferenceException, NodeException;

	/**
	 * Yields the class loader used to build the response.
	 * 
	 * @return the class loader
	 */
	EngineClassLoader getClassLoader();
}