package io.hotmoka.network.internal.services;

import org.springframework.http.ResponseEntity;

public interface NodePostService {
    ResponseEntity<Object> postJarStoreTransaction();
    ResponseEntity<Object> postConstructorCallTransaction();
    ResponseEntity<Object> postInstanceMethodCallTransaction();
    ResponseEntity<Object> postStaticMethodCallTransaction();
}
