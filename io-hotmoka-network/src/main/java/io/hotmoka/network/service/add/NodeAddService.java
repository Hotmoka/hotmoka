package io.hotmoka.network.service.add;

import io.hotmoka.network.model.transaction.*;
import org.springframework.http.ResponseEntity;

public interface NodeAddService {
    ResponseEntity<Object> addJarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request);
    ResponseEntity<Object> addGameteCreationTransaction(GameteCreationTransactionRequestModel request);
    ResponseEntity<Object> addRedGreenGameteCreationTransaction(RGGameteCreationTransactionRequestModel request);
    ResponseEntity<Object> addInitializationTransaction(TransactionModel request);
    ResponseEntity<Object> addJarStoreTransaction(JarStoreTransactionRequestModel request);
    ResponseEntity<Object> addConstructorCallTransaction();
    ResponseEntity<Object> addInstanceMethodCallTransaction();
    ResponseEntity<Object> addStaticMethodCallTransaction();
}
