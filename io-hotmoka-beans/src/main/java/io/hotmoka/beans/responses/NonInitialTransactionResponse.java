package io.hotmoka.beans.responses;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.TransactionResponse;

/**
 * A response for a non-initial transaction.
 */
@Immutable
public interface NonInitialTransactionResponse extends TransactionResponse {
}