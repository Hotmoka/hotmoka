package io.takamaka.code.engine.internal;

import io.hotmoka.beans.references.TransactionReference;
import io.takamaka.code.engine.Store;
import io.takamaka.code.engine.internal.transactions.AbstractNodeProxyForTransactions;

/**
 * The methods of an abstract node that are only used inside this package.
 * By using this proxy class, we avoid to define them as public.
 */
public abstract class AbstractNodeProxyForEngine<S extends Store> extends AbstractNodeProxyForTransactions<S> {

	@Override
	protected final EngineClassLoader mkClassLoader(TransactionReference classpath) throws Exception {
		return new EngineClassLoader(classpath, this);
	}

	/**
	 * Yields the store of this node.
	 * 
	 * @return the store of this node
	 */
	protected abstract S getStore();

	@Override
	public void close() throws Exception {
		S store = getStore();
		if (store != null)
			store.close();
	}
}