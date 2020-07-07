package io.hotmoka.network.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import io.hotmoka.network.Application;
import io.hotmoka.network.Config;
import io.hotmoka.network.NodeService;
import io.hotmoka.nodes.Node;

/**
 * A simple web service that exposes some REST APIs to access an instance of a {@link io.hotmoka.nodes.Node}.
 */
public class NodeServiceImpl implements NodeService {
	private final ConfigurableApplicationContext context;
    private final static Logger LOGGER = LoggerFactory.getLogger(NodeServiceImpl.class);

	/**
	 * Yields an implementation of a network service that exposes a REST API to a given Hotmoka node.
	 * 
	 * @param config the configuration of the network
	 * @param node the Hotmoka node
	 * @return the network service implementation
	 */
    public NodeServiceImpl(Config config, Node node) {
    	Application application = new Application();
        context = application.start(config, node);
        LOGGER.info("Network server for Hotmoka node started");
    }

    @Override
    public void close() {
    	SpringApplication.exit(context);
    	LOGGER.info("Network server for Hotmoka node closed");
    }
}