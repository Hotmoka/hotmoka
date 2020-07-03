package io.hotmoka.network.service.add;

import io.hotmoka.network.model.transaction.GameteCreationTransactionRequestModel;
import io.hotmoka.network.model.transaction.TransactionRequestModel;
import org.springframework.http.ResponseEntity;

public interface NodeAddService {
    ResponseEntity<Object> addJarStoreInitialTransaction(TransactionRequestModel transactionRequestModel);
    ResponseEntity<Object> addGameteCreationTransaction(GameteCreationTransactionRequestModel request);
    ResponseEntity<Object> addRedGreenGameteCreationTransaction();
    ResponseEntity<Object> addInitializationTransaction();
    ResponseEntity<Object> addJarStoreTransaction();
    ResponseEntity<Object> addConstructorCallTransaction();
    ResponseEntity<Object> addInstanceMethodCallTransaction();
    ResponseEntity<Object> addStaticMethodCallTransaction();
}
