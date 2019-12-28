package io.takamaka.code.blockchain.responses;

import io.takamaka.code.blockchain.annotations.Immutable;

/**
 * A response for a non-initial transaction.
 */
@Immutable
public interface NonInitialTransactionResponse extends TransactionResponse {
}