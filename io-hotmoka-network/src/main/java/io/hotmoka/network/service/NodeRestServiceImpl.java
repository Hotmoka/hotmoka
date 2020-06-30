package io.hotmoka.network.service;

import io.hotmoka.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class NodeRestServiceImpl extends NetworkService implements NodeRestService {
    private final static Logger LOGGER = LoggerFactory.getLogger(NodeRestServiceImpl.class);

    @Autowired
    private ApplicationContext applicationContext;

    public ResponseEntity<Object> getTakamakaCode() {

        try {

            Node node = (Node) this.applicationContext.getBean("node");
            assertNodeNotNull(node);

            return response(node.getTakamakaCode());

        }catch (Exception e) {
            LOGGER.error("getTakamakaCode", e);
            return exceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Object> getManifest() {
        try {

            Node node = (Node) this.applicationContext.getBean("node");
            assertNodeNotNull(node);

            return response(node.getManifest());

        } catch (Exception e) {
            LOGGER.error("getManifest", e);
            return exceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Object> getState() {
        try {

            Node node = (Node) this.applicationContext.getBean("node");
            assertNodeNotNull(node);

           return response(node.getState(node.getManifest()));

        } catch (Exception e) {
            LOGGER.error("getState", e);
            return exceptionResponse(e);
        }
    }
}
