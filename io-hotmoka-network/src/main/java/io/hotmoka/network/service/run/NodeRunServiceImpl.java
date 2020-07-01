package io.hotmoka.network.service.run;

import io.hotmoka.network.service.NetworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class NodeRunServiceImpl extends NetworkService implements NodeRunService {
    private final static Logger LOGGER = LoggerFactory.getLogger(NodeRunServiceImpl.class);

    @Override
    public ResponseEntity<Object> runInstanceMethodCallTransaction() {
        return null;
    }

    @Override
    public ResponseEntity<Object> runStaticMethodCallTransaction() {
        return null;
    }
}
