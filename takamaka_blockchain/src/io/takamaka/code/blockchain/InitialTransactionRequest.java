package io.takamaka.code.blockchain;

import io.takamaka.code.blockchain.annotations.Immutable;
import io.takamaka.code.blockchain.request.TransactionRequest;

@Immutable
public interface InitialTransactionRequest extends TransactionRequest {
}