package io.takamaka.code.engine;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;

/**
 * The creator of a response from a request. It executes a transaction from the request and builds the corresponding response.
 * 
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
	 * Dumps all reverified responses into the store of the node for which the response is built.
	 */
	void pushReverification();

	/**
	 * Yields the class loader used to build the response.
	 * 
	 * @return the class loader
	 */
	EngineClassLoader getClassLoader();
}