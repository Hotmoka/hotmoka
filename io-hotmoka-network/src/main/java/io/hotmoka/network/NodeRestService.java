package io.hotmoka.network;

import io.hotmoka.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Simple web application with Tomcat embedded which exposes some REST APIs to access an instance of a node {@link io.hotmoka.nodes.Node}
 */
@SpringBootApplication
public class NodeRestService {
    private final static Logger LOGGER = LoggerFactory.getLogger(NodeRestService.class);

    private final String[] args;
    private ConfigurableApplicationContext configurableApplicationContext;

    public static void main(String[] args) {
        // needed by spring
        throw new IllegalStateException("Cannot run NodeRestService as a standalone jar");
    }

    public NodeRestService(String[] args) {
        this.args = args;
    }

    public NodeRestService() {
        this(new String[]{});
    }

    /**
     * Start the web application and register the node {@link io.hotmoka.nodes.Node} to the beans map of the Spring framework in order
     * to be accessible by the application
     * @param node the {@link io.hotmoka.nodes.Node} instance to be accessed by the REST APIs
     */
    public void start(Node node) {
        LOGGER.info("Starting NodeRestService");

        this.configurableApplicationContext = SpringApplication.run(NodeRestService.class, args);
        this.configurableApplicationContext.getBeanFactory().registerSingleton("node", node);
    }

    /**
     * Shutdown the web service instance
     */
    public void stop() {
        LOGGER.info("Stopping NodeRestService");
        SpringApplication.exit(configurableApplicationContext);
    }
}
