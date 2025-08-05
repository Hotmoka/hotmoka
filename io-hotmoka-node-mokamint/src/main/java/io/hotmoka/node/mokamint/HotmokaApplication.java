package io.hotmoka.node.mokamint;

import io.hotmoka.node.mokamint.api.MokamintNodeConfig;
import io.hotmoka.node.mokamint.internal.HotmokaApplicationImpl;
import io.mokamint.node.api.PublicNode;

/**
 * Implementation of an application for the Mokamint engine, that supports a Hotmoka node.
 * Its constructor creates the Hotmoka node as well, which is later accessible
 * via the {@link #getNode()} method.
 * 
 * @param <E> the type of the underlying Mokamint engine
 */
public class HotmokaApplication<E extends PublicNode> extends HotmokaApplicationImpl<E> {

	/**
	 * Creates a Mokamint application that supports a Hotmoka node.
	 * 
	 * @param config the configuration of the Hotmoka node
	 * @param init if true, the working directory of the node gets initialized
	 */
	public HotmokaApplication(MokamintNodeConfig config, boolean init) {
		super(config, init);
	}
}