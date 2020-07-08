package io.hotmoka.network.internal.services;

import io.hotmoka.network.internal.models.storage.StorageModel;
import org.springframework.http.ResponseEntity;

public interface NodeGetService {
    ResponseEntity<Object> getTakamakaCode();
    ResponseEntity<Object> getManifest();
    ResponseEntity<Object> getState(StorageModel request);
    ResponseEntity<Object> getClassTag(StorageModel request);
}
