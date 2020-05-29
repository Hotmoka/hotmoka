package io.hotmoka.beans.requests;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.InitialTransactionResponse;

/**
 * A request for a transaction that can only be executed before the initialization of a node.
 */
@Immutable
public abstract class InitialTransactionRequest<R extends InitialTransactionResponse> extends TransactionRequest<R> {
}