package io.hotmoka.service;

import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.service.internal.http.HTTPRemoteNodeImpl;
import io.hotmoka.service.internal.websockets.WebSocketsRemoteNodeImpl;
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
		// there are two implementations: for websockets or for http connections
		return config.webSockets ? new WebSocketsRemoteNodeImpl(config) : new HTTPRemoteNodeImpl(config);
	}
}