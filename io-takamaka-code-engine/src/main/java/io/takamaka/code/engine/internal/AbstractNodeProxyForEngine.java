package io.takamaka.code.engine.internal;

import io.hotmoka.beans.references.TransactionReference;
import io.takamaka.code.engine.Store;
import io.takamaka.code.engine.internal.transactions.AbstractNodeProxyForTransactions;
import io.takamaka.code.instrumentation.GasCostModel;

/**
 * The methods of an abstract node that are only used inside this package.
 * By using this proxy class, we avoid to define them as public.
 */
public abstract class AbstractNodeProxyForEngine extends AbstractNodeProxyForTransactions {

	/**
	 * The default gas model of the node.
	 */
	private final static GasCostModel defaultGasCostModel = GasCostModel.standard();

	@Override
	protected final EngineClassLoader mkClassLoader(TransactionReference classpath) throws Exception {
		return new EngineClassLoader(classpath, this);
	}

	@Override
	protected abstract Store<?> getStore();

	/**
	 * Yields the gas cost model of this node.
	 * 
	 * @return the default gas cost model. Subclasses may redefine
	 */
	public GasCostModel getGasCostModel() {
		return defaultGasCostModel;
	}
}