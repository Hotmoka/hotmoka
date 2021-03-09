package io.hotmoka.service.internal.services;

import io.hotmoka.service.models.requests.*;
import io.hotmoka.service.models.values.StorageReferenceModel;
import io.hotmoka.service.models.values.StorageValueModel;
import io.hotmoka.service.models.values.TransactionReferenceModel;

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