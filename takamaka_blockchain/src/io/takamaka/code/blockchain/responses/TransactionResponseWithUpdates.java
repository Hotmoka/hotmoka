package io.takamaka.code.blockchain.responses;

import java.util.stream.Stream;

import io.takamaka.code.blockchain.annotations.Immutable;
import io.takamaka.code.blockchain.updates.Update;

/**
 * A response for a transaction that might contain updates.
 */
@Immutable
public interface TransactionResponseWithUpdates extends TransactionResponse {
	
	/**
	 * Yields the updates induced by the execution of this transaction.
	 * 
	 * @return the updates
	 */
	public Stream<Update> getUpdates();
}
