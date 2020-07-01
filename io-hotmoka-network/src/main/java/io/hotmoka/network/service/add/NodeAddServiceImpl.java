package io.hotmoka.network.service.add;

import io.hotmoka.network.service.NetworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class NodeAddServiceImpl extends NetworkService implements NodeAddService {
    private final static Logger LOGGER = LoggerFactory.getLogger(NodeAddServiceImpl.class);

    @Override
    public ResponseEntity<Object> addJarStoreInitialTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> addGameteCreationTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> addRedGreenGameteCreationTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> addInitializationTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> addJarStoreTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> addConstructorCallTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> addInstanceMethodCallTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> addStaticMethodCallTransaction() {
        return null;
    }
}
