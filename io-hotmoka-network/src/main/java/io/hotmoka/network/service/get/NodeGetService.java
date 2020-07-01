package io.hotmoka.network.service.get;

import org.springframework.http.ResponseEntity;

public interface NodeGetService {
    ResponseEntity<Object> getTakamakaCode();
    ResponseEntity<Object> getManifest();
    ResponseEntity<Object> getState();
}
