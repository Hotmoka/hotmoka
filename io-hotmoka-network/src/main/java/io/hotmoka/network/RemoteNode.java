package io.hotmoka.network;

import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.network.internal.RemoteNodeImpl;
import io.hotmoka.network.internal.WsRemoteNodeImpl;
import io.hotmoka.nodes.Node;

/**
 * A node that forwards its calls to a remote network service.
 */
@ThreadSafe
public interface RemoteNode extends Node {

	/**
	 * Yields a remote node with the given configuration.
	 * 
	 * @param config the configuration
	 * @return the remote node
	 */
	static RemoteNode of(RemoteNodeConfig config) {
		// there are two implementations, for websockets
		// or for http connections
		if (config.webSockets)
			return new WsRemoteNodeImpl(config);
		else
			return new RemoteNodeImpl(config);
	}
}