package io.hotmoka.nodes;

import java.util.NoSuchElementException;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;

/**
 * A node of the Hotmoka network, that provides the storage
 * facilities for the execution of Takamaka code.
 * Calls to code in the node can be added, run or posted.
 * Posted calls are executed, eventually, and their value can be retrieved
 * through the future returned by the calls. Added calls are shorthand
 * for posting a call and waiting until the value of their future is
 * available. Run calls are only available for view methods, without side-effects.
 * They execute immediately and never modify the store of the node.
 */
public interface NodeWithHistory extends Node {

	/**
	 * Yields the request that generated the transaction with the given reference.
	 * If this node has some form of commit, then this method can only succeed
	 * when the transaction has been definitely committed in this node.
	 * 
	 * @param reference the reference of the transaction
	 * @return the request
	 * @throws NoSuchElementException if there is no request with that reference
	 */
	TransactionRequest<?> getRequestAt(TransactionReference reference) throws NoSuchElementException;

	/**
	 * Yields the response generated for the request for the given transaction.
	 * If this node has some form of commit, then this method can only succeed
	 * or yield a {@linkplain TransactionRejectedException} only
	 * when the transaction has been definitely committed in this node.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response
	 * @throws TransactionRejectedException if there is a request for that transaction but it failed with this exception
	 * @throws NoSuchElementException if there is no request, and hence no response, with that reference
	 */
	TransactionResponse getResponseAt(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException;
}