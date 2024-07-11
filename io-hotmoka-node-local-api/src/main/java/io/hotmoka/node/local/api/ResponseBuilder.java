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

import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;

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
	 * Yields the response corresponding to the request for which this builder was created.
	 * 
	 * @return the response
	 * @throws TransactionRejectedException if the response cannot be computed because the request
	 *                                      is inconsistent in the context of execution
	 * @throws StoreException if the node is misbehaving
	 * @throws InterruptedException if the current thread has been interrupted before computation the response
	 */
	Response getResponse() throws TransactionRejectedException, StoreException, InterruptedException;
	
	/**
	 * Replaces all reverified responses into the store for which the response is built.
	 * 
	 * @throws StoreException if the operation did not complete correctly
	 */
	void replaceReverifiedResponses() throws StoreException;

	/**
	 * Yields the class loader used to build the response.
	 * 
	 * @return the class loader
	 */
	EngineClassLoader getClassLoader();
}