package io.takamaka.code.engine.internal;

import io.hotmoka.beans.references.TransactionReference;
import io.takamaka.code.engine.internal.transactions.AbstractNodeProxyForTransactions;

/**
 * The methods of an abstract node that are only used inside this package.
 * By using this proxy class, we avoid to define them as public.
 */
public abstract class AbstractNodeProxyForEngine extends AbstractNodeProxyForTransactions {

	@Override
	protected final EngineClassLoader mkClassLoader(TransactionReference classpath) throws Exception {
		return new EngineClassLoader(classpath, this);
	}
}