package io.hotmoka.network.service.post;

import io.hotmoka.network.service.NetworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class NodePostServiceImpl extends NetworkService implements NodePostService {
    private final static Logger LOGGER = LoggerFactory.getLogger(NodePostServiceImpl.class);

    @Override
    public ResponseEntity<Object> postJarStoreTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> postConstructorCallTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> postInstanceMethodCallTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> postStaticMethodCallTransaction() {
        return null;
    }
}
