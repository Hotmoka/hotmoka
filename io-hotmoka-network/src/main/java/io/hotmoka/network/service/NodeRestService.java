package io.hotmoka.network.service;

import org.springframework.http.ResponseEntity;

public interface NodeRestService {

    ResponseEntity<Object> getTakamakaCode();
    ResponseEntity<Object> getManifest();
    ResponseEntity<Object> getState();
}
