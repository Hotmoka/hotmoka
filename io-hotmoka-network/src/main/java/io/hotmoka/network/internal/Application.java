package io.hotmoka.network.internal;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.hotmoka.nodes.Node;

/**
 * A simple Spring boot application. Its annotation specifies
 * that Spring's components must be looked up in this package and in its subpackages.
 */
@SpringBootApplication
public class Application {

	/**
	 * The Hotmoka node exposed by this application.
	 */
	private Node node;

	/**
	 * Sets the Hotmoka node exposed by this application.
	 * 
	 * @param node the Hotmoka node
	 */
	void setNode(Node node) {
		this.node = node;
	}

	/**
	 * Yields the Hotmoka node exposed by this application.
	 * 
	 * @return the Hotmoka node
	 */
	public Node getNode() {
		return node;
	}
}