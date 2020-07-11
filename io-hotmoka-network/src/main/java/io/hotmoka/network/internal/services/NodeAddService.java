package io.hotmoka.network.internal.services;

import io.hotmoka.network.internal.models.transactions.*;

import org.springframework.http.ResponseEntity;

public interface NodeAddService {
    ResponseEntity<Object> addJarStoreInitialTransaction(JarStoreInitialTransactionRequestModel request);
    ResponseEntity<Object> addGameteCreationTransaction(GameteCreationTransactionRequestModel request);
    ResponseEntity<Object> addRedGreenGameteCreationTransaction(RGGameteCreationTransactionRequestModel request);
    ResponseEntity<Object> addInitializationTransaction(InitializationTransactionRequestModel request);
    ResponseEntity<Object> addJarStoreTransaction(JarStoreTransactionRequestModel request);
    ResponseEntity<Object> addConstructorCallTransaction(ConstructorCallTransactionRequestModel request);
    ResponseEntity<Object> addInstanceMethodCallTransaction(MethodCallTransactionRequestModel request);
    ResponseEntity<Object> addStaticMethodCallTransaction(MethodCallTransactionRequestModel request);
}
