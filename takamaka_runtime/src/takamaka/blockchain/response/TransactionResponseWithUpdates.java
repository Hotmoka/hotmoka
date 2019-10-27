package takamaka.blockchain.response;

import java.util.stream.Stream;

import io.takamaka.annotations.Immutable;
import takamaka.blockchain.Update;

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
