package io.hotmoka.network.service.add;

import org.springframework.http.ResponseEntity;

public interface NodeAddService {
    ResponseEntity<Object> addJarStoreInitialTransaction();
    ResponseEntity<Object> addGameteCreationTransaction();
    ResponseEntity<Object> addRedGreenGameteCreationTransaction();
    ResponseEntity<Object> addInitializationTransaction();
    ResponseEntity<Object> addJarStoreTransaction();
    ResponseEntity<Object> addConstructorCallTransaction();
    ResponseEntity<Object> addInstanceMethodCallTransaction();
    ResponseEntity<Object> addStaticMethodCallTransaction();
}
