package io.takamaka.code.engine;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;

/**
 * The creator of a response from a request. It executes a transaction from the request and builds the corresponding response.
 * The factory methods in this interface check the prerequisite for running the
 * transaction, such as the fact that the caller can be identified and has provided a minimum of gas.
 * The factory methods can be executed in a thread-safe way, since they do not depend on
 * information in the node's store that might be modified by other transactions.
 * If these factory methods fail, then a node could for instance reject the request.
 * The {@link #build()} method performs the actual creation of the response and
 * depends on the current store of the node. Hence, {@link #build()}
 * is not thread-safe and should be executed only after a lock is taken on the store of the node.
 * 
 * @param <Response> the type of the response of the transaction
 */
public interface ResponseBuilder<Request extends TransactionRequest<Response>, Response extends TransactionResponse> {

	/**
	 * Builds the response of the transaction.
	 * 
	 * @return the response
	 * @throws TransactionRejectedException if the response cannot be built
	 */
	Response build() throws TransactionRejectedException;

	/**
	 * Yield the request for which this builder was created.
	 * 
	 * @return the request
	 */
	Request getRequest();
}