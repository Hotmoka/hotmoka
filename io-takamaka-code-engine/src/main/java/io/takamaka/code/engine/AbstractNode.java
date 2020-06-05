package io.takamaka.code.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.internal.AbstractNodeProxyForEngine;

/**
 * A generic implementation of a node.
 * Specific implementations can subclass this and implement the abstract template methods.
 */
public abstract class AbstractNode<C extends Config> extends AbstractNodeProxyForEngine implements Node {
	private final static Logger logger = LoggerFactory.getLogger(AbstractNode.class);

	/**
	 * Builds the node.
	 * 
	 * @param config the configuration of the node
	 */
	protected AbstractNode(C config) {
		super();
	}
}