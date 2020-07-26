package io.hotmoka.network.internal.services;

import io.hotmoka.network.internal.models.requests.*;
import io.hotmoka.network.internal.models.values.StorageReferenceModel;
import io.hotmoka.network.internal.models.values.StorageValueModel;
import io.hotmoka.network.internal.models.values.TransactionReferenceModel;

import org.springframework.http.ResponseEntity;

public interface AddService {
    TransactionReferenceModel addJarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request);
    StorageReferenceModel addGameteCreationTransaction(GameteCreationTransactionRequestModel request);
    StorageReferenceModel addRedGreenGameteCreationTransaction(RGGameteCreationTransactionRequestModel request);
    ResponseEntity<Void> addInitializationTransaction(InitializationTransactionRequestModel request);
    TransactionReferenceModel addJarStoreTransaction(JarStoreTransactionRequestModel request);
    StorageReferenceModel addConstructorCallTransaction(ConstructorCallTransactionRequestModel request);
    StorageValueModel addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request);
    StorageValueModel addStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request);
}