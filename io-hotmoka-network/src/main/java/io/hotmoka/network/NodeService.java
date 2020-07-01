package io.hotmoka.network;

import io.hotmoka.nodes.Node;

/**
 * Simple web service which exposes some REST APIs to access an instance of a node {@link io.hotmoka.nodes.Node}
 */
public class NodeService {
    private final Application application;
    private final Config config;
    private final Node node;

    public NodeService(Config config, Node node) {
        this.config = config;
        this.node = node;
        this.application = new Application();
    }

    public NodeService(Node node) {
        this(null, node);
    }

    /**
     * Start the web service
     */
    public void start() {
    	application.start(config, node);
    }

    /**
     * Shutdown the web service
     */
    public void stop() {
    	application.stop();
    }
}
