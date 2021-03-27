package io.hotmoka.service.internal.services;

import io.hotmoka.network.requests.*;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.network.values.StorageValueModel;
import io.hotmoka.network.values.TransactionReferenceModel;

import org.springframework.http.ResponseEntity;

public interface AddService {
    TransactionReferenceModel addJarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request);
    StorageReferenceModel addGameteCreationTransaction(GameteCreationTransactionRequestModel request);
    ResponseEntity<Void> addInitializationTransaction(InitializationTransactionRequestModel request);
    TransactionReferenceModel addJarStoreTransaction(JarStoreTransactionRequestModel request);
    StorageReferenceModel addConstructorCallTransaction(ConstructorCallTransactionRequestModel request);
    StorageValueModel addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequestModel request);
    StorageValueModel addStaticMethodCallTransaction(StaticMethodCallTransactionRequestModel request);
}