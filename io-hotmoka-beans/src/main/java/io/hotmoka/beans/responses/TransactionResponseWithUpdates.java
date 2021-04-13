package io.hotmoka.beans.responses;

import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.updates.Update;

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
    Stream<Update> getUpdates();
}