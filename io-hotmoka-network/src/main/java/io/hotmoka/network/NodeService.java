package io.hotmoka.network;

import io.hotmoka.nodes.Node;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple web service which exposes some REST APIs to access an instance of a node {@link io.hotmoka.nodes.Node}
 */
public class NodeService {
    private final Application application;
    private final Config config;
    private final Node node;
    private ExecutorService executor;

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
        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> this.application.start(this.config, this.node));
    }

    /**
     * Shutdown the web service
     */
    public void stop() {
        this.application.stop();
        this.executor.shutdown();
    }
}
