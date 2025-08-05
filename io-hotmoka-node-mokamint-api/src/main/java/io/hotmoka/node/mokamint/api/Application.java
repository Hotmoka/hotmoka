package io.hotmoka.node.mokamint.api;

/**
 * An application for the Mokamint engine, that supports a Hotmoka node.
 * It is connected to the Hotmoka node, which is accessible via the {@link #getNode()} method.
 */
public interface Application extends io.mokamint.application.api.Application {

	/**
	 * Yields the Hotmoka node connected to this application.
	 * 
	 * @return the Hotmoka node connected to this application
	 */
	MokamintNode getNode();
}