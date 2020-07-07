package io.hotmoka.network;

import io.hotmoka.network.internal.NodeServiceImpl;
import io.hotmoka.nodes.Node;

/**
 * A simple web service that exposes some REST APIs to access an instance of a {@link io.hotmoka.nodes.Node}.
 */
public interface NodeService extends AutoCloseable {

	static NodeService of(Config config, Node node) {
		return new NodeServiceImpl(config, node);
	}

	@Override
    public void close(); // no checked exceptions
}