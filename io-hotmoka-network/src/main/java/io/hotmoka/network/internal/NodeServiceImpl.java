package io.hotmoka.network.internal;

import io.hotmoka.network.Application;
import io.hotmoka.network.Config;
import io.hotmoka.network.NodeService;
import io.hotmoka.nodes.Node;

/**
 * A simple web service that exposes some REST APIs to access an instance of a {@link io.hotmoka.nodes.Node}.
 */
public class NodeServiceImpl implements NodeService {
    private final Application application;

	/**
	 * Yields an implementation of a network service that exposes a REST API to a given Hotmoka node.
	 * 
	 * @param config the configuration of the network
	 * @param node the Hotmoka node
	 * @return the network service implementation
	 */
    public NodeServiceImpl(Config config, Node node) {
        this.application = new Application();
        this.application.start(config, node);
    }

    @Override
    public void close() {
    	application.stop();
    }
}