package io.hotmoka.network.service.run;

import org.springframework.http.ResponseEntity;

public interface NodeRunService {
    ResponseEntity<Object> runInstanceMethodCallTransaction();
    ResponseEntity<Object> runStaticMethodCallTransaction();
}
