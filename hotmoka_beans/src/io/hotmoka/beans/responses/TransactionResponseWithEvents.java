package io.hotmoka.beans.responses;

import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;

/**
 * A response for a transaction that might contain events.
 */
@Immutable
public interface TransactionResponseWithEvents extends TransactionResponse {
	
	/**
	 * Yields the events induced by the execution of this transaction.
	 * 
	 * @return the events
	 */
	public Stream<StorageReference> getEvents();
}
