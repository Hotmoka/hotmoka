package io.hotmoka.network.service;

import io.hotmoka.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NodeRestServiceImpl {
    private final static Logger LOGGER = LoggerFactory.getLogger(NodeRestServiceImpl.class);

    @Autowired
    private ApplicationContext applicationContext;

    public Object getTakamakaCode() {

        try {

            Node node = (Node) this.applicationContext.getBean("node");
            return node.getTakamakaCode();

        } catch (Exception e) {
            LOGGER.error("getTakamakaCode", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Node error", e);
        }
    }
}
