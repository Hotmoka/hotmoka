package io.takamaka.code.engine;

import io.hotmoka.nodes.GasCostModel;
import io.hotmoka.nodes.Node;

/**
 * A generic implementation of a HotMoka node. Specific implementations can subclass this class
 * and just implement the remaining missing abstract template methods.
 */
public abstract class AbstractNode implements Node {

	private final static GasCostModel defaultGasCostModel = GasCostModel.standard();

	@Override
	public GasCostModel getGasCostModel() {
		return defaultGasCostModel;
	}
}