package io.hotmoka.network.internal.services;

import org.springframework.http.ResponseEntity;

public interface NodeGetService {
    ResponseEntity<Object> getTakamakaCode();
    ResponseEntity<Object> getManifest();
    ResponseEntity<Object> getState();
    ResponseEntity<Object> getClassTag();
}
