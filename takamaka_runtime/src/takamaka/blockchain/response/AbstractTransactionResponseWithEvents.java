package takamaka.blockchain.response;

import java.util.stream.Stream;

import takamaka.blockchain.values.StorageReference;
import takamaka.lang.Immutable;

/**
 * A response for a transaction that might contain events.
 */
@Immutable
public interface AbstractTransactionResponseWithEvents {
	
	/**
	 * Yields the events induced by the execution of this transaction.
	 * 
	 * @return the events
	 */
	public Stream<StorageReference> getEvents();
}
