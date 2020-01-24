package io.takamaka.code.engine;

import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;

/**
 * A transaction of HotMoka code: it is the execution of a
 * request, that led to a response.
 *
 * @param <R> the type of the response of this transaction
 */
public interface Transaction<R extends TransactionResponse> {

	/**
	 * The request from where this transaction started
	 * 
	 * @return the request
	 */
	TransactionRequest<R> getRequest();

	/**
	 * The response into which this transaction led
	 * 
	 * @return the response
	 */
	R getResponse();
}