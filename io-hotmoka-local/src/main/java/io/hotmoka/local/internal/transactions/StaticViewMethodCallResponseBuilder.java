package io.hotmoka.local.internal.transactions;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.local.ViewResponseBuilder;
import io.hotmoka.local.internal.NodeInternal;

/**
 * The builder of the response for a transaction that executes a static method of Takamaka code
 * annotated as {@link io.takamaka.code.lang.View}.
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
	public StaticViewMethodCallResponseBuilder(TransactionReference reference, StaticMethodCallTransactionRequest request, NodeInternal node) throws TransactionRejectedException {
		super(reference, request, node);
	}
}