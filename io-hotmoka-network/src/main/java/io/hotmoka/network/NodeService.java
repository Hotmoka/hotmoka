package io.hotmoka.network;

import io.hotmoka.network.internal.NodeServiceImpl;
import io.hotmoka.nodes.Node;

/**
 * A network service that exposes a REST API to a Hotmoka node.
 */
public interface NodeService extends AutoCloseable {

	/**
	 * Yields and starts network service that exposes a REST API to a given Hotmoka node.
	 * 
	 * @param config the configuration of the network
	 * @param node the Hotmoka node
	 * @return the network service
	 */
	static NodeService of(NodeServiceConfig config, Node node) {
		return new NodeServiceImpl(config, node);
	}

	/**
	 * Stops the service and releases its resources.
	 */
	@Override
	void close(); // no checked exceptions
}