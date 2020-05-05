package io.takamaka.code.engine.internal.transactions;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.takamaka.code.engine.AbstractNode;

/**
 * The builder of the response for a transaction that executes a static method of Takamaka code
 * annotated as {@linkplain io.hotmoka.code.lang.View}.
 */
public class StaticViewMethodCallResponseBuilder extends StaticMethodCallResponseBuilder implements ViewResponseBuilder {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public StaticViewMethodCallResponseBuilder(StaticMethodCallTransactionRequest request, AbstractNode<?> node) throws TransactionRejectedException {
		super(request, node);
	}
}