package io.takamaka.code.engine.internal.transactions;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.takamaka.code.engine.AbstractNode;
import io.takamaka.code.engine.ViewResponseBuilder;

/**
 * The builder of the response for a transaction that executes a static method of Takamaka code
 * annotated as {@linkplain io.hotmoka.code.lang.View}.
 */
public class StaticViewMethodCallResponseBuilder extends StaticMethodCallResponseBuilder implements ViewResponseBuilder {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public StaticViewMethodCallResponseBuilder(TransactionReference reference, StaticMethodCallTransactionRequest request, AbstractNode<?,?> node) throws TransactionRejectedException {
		super(reference, request, node);
	}
}