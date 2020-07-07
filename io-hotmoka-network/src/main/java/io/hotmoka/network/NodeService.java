package io.hotmoka.network;

import io.hotmoka.network.internal.NodeServiceImpl;
import io.hotmoka.nodes.Node;

/**
 * A network service that exposes a REST API to a Hotmoka node.
 */
public interface NodeService extends AutoCloseable {

	/**
	 * Yields a network service that exposes a REST API to a given Hotmoka node.
	 * 
	 * @param config the configuration of the network
	 * @param node the Hotmoka node
	 * @return the network service
	 */
	static NodeService of(Config config, Node node) {
		return new NodeServiceImpl(config, node);
	}

	@Override
	void close(); // no checked exceptions
}