package io.hotmoka.node.mokamint;

import io.hotmoka.node.mokamint.api.MokamintNodeConfig;
import io.hotmoka.node.mokamint.internal.HotmokaApplicationImpl;

/**
 * Implementation of an application for the Mokamint engine, that supports a Hotmoka node.
 * Its constructor creates the Hotmoka node as well, which is later accessible
 * via the {@link #getNode()} method.
 */
public class HotmokaApplication extends HotmokaApplicationImpl {

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