package io.takamaka.code.blockchain.response;

import java.util.stream.Stream;

import io.takamaka.code.blockchain.annotations.Immutable;
import io.takamaka.code.blockchain.values.StorageReference;

/**
 * A response for a transaction that might contain events.
 */
@Immutable
public interface TransactionResponseWithEvents {
	
	/**
	 * Yields the events induced by the execution of this transaction.
	 * 
	 * @return the events
	 */
	public Stream<StorageReference> getEvents();
}
