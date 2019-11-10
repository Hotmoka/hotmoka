package io.takamaka.code.blockchain.response;

import java.util.stream.Stream;

import io.takamaka.code.blockchain.Update;
import io.takamaka.code.blockchain.annotations.Immutable;

/**
 * A response for a transaction that might contain updates.
 */
@Immutable
public interface TransactionResponseWithUpdates {
	
	/**
	 * Yields the updates induced by the execution of this transaction.
	 * 
	 * @return the updates
	 */
	public Stream<Update> getUpdates();
}
