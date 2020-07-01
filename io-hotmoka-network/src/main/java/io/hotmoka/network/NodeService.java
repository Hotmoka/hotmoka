package io.hotmoka.network;

import io.hotmoka.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Simple web service which exposes some REST APIs to access an instance of a node {@link io.hotmoka.nodes.Node}
 */
public class NodeService {
    private final static Logger LOGGER = LoggerFactory.getLogger(NodeService.class);

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
        Future<Void> future = this.executor.submit(() -> {
            this.application.stop();
            return null;
        });

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Errore stopping NodeService", e);
        } finally {
            this.executor.shutdown();
        }
    }
}
