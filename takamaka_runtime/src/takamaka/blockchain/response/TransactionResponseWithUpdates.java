package takamaka.blockchain.response;

import java.util.stream.Stream;

import takamaka.blockchain.Update;
import takamaka.lang.Immutable;

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
