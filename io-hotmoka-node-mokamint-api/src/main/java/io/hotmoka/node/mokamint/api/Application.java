package io.hotmoka.node.mokamint.api;

import io.mokamint.node.api.PublicNode;

/**
 * An application for the Mokamint engine, that supports a Hotmoka node.
 * It is connected to the Hotmoka node, which is accessible via the {@link #getNode()} method.
 * 
 *  @param <E> the type of the underlying Mokamint engine
 */
public interface Application<E extends PublicNode> extends io.mokamint.application.api.Application {

	/**
	 * Yields the Hotmoka node connected to this application.
	 * 
	 * @return the Hotmoka node connected to this application
	 */
	MokamintNode<E> getNode();
}