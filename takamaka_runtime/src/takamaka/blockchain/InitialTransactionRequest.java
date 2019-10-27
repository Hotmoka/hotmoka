package takamaka.blockchain;

import io.takamaka.annotations.Immutable;
import takamaka.blockchain.request.TransactionRequest;

@Immutable
public interface InitialTransactionRequest extends TransactionRequest {
}