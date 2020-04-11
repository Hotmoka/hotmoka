package io.takamaka.code.engine;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.responses.TransactionResponse;

/**
 * The creator of a response from a request. It executes a transaction from the request and builds the corresponding response.
 * The constructors of the implementations of this interface check the prerequisite for running the
 * transaction, such as the fact that the caller can be identified and has provided a minimum of gas.
 * The {@linkplain #build()} method, instead, performs the actual creation of the response.
 * If the constructors fail, then a node could for instance reject the transaction.
 * 
 * @param <Response> the type of the response of the transaction
 */
public interface ResponseBuilder<Response extends TransactionResponse> {

	/**
	 * Builds the response of the transaction.
	 * 
	 * @return the response
	 * @throws TransactionRejectedException if the response cannot be built
	 */
	Response build() throws TransactionRejectedException;
}