package io.hotmoka.network.service.post;

import org.springframework.http.ResponseEntity;

public interface NodePostService {
    ResponseEntity<Object> postJarStoreTransaction();
    ResponseEntity<Object> postConstructorCallTransaction();
    ResponseEntity<Object> postInstanceMethodCallTransaction();
    ResponseEntity<Object> postStaticMethodCallTransaction();
}
