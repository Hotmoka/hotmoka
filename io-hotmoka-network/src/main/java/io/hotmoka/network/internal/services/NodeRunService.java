package io.hotmoka.network.internal.services;

import org.springframework.http.ResponseEntity;

public interface NodeRunService {
    ResponseEntity<Object> runInstanceMethodCallTransaction();
    ResponseEntity<Object> runStaticMethodCallTransaction();
}
