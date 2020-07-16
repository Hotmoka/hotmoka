package io.hotmoka.takamaka;

import java.util.stream.Stream;

import io.hotmoka.beans.responses.TransactionResponse;

/**
 * The result of the execution of a delta group.
 */
public interface DeltaGroupExecutionResult {

	/**
	 * Yields the hash of the store that points to its view at the end of the
	 * execution of the requests in the delta group.
	 * 
	 * @return the hash
	 */
	byte[] getHash();

	/**
	 * Yields an ordered stream of the responses of the requests. If some request could
	 * not be executed because is syntactical errors in the request, its position contains {@code null}.
	 * For requests that could be executed but failed in the user code of the smart contracts,
	 * their position will contain a failed transaction response object, but never {@code null}.
	 * 
	 * @return the responses, in the same order as they were scheduled for execution
	 */
	Stream<TransactionResponse> responses();

	/**
	 * Yields the identifier of the execution whose result is this.
	 * 
	 * @return the identifier
	 */
	String getId();
}