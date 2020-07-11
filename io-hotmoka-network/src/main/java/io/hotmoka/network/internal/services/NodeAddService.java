package io.hotmoka.network.internal.services;

import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.models.transactions.*;
import org.springframework.http.ResponseEntity;

public interface NodeAddService {
    TransactionReferenceModel addJarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request);
    StorageReferenceModel addGameteCreationTransaction(GameteCreationTransactionRequestModel request);
    StorageReferenceModel addRedGreenGameteCreationTransaction(RGGameteCreationTransactionRequestModel request);
    ResponseEntity<Object> addInitializationTransaction(InitializationTransactionRequestModel request);
    TransactionReferenceModel addJarStoreTransaction(JarStoreTransactionRequestModel request);
    StorageReferenceModel addConstructorCallTransaction(ConstructorCallTransactionRequestModel request);
    StorageValueModel addInstanceMethodCallTransaction(MethodCallTransactionRequestModel request);
    StorageValueModel addStaticMethodCallTransaction(MethodCallTransactionRequestModel request);
}
