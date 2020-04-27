package io.hotmoka.beans.requests;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.InitialTransactionResponse;

@Immutable
public abstract class InitialTransactionRequest<R extends InitialTransactionResponse> extends TransactionRequest<R> {
}