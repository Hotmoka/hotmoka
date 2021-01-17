package io.takamaka.code.engine.internal.transactions;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.responses.InitializationTransactionResponse;
import io.takamaka.code.engine.AbstractLocalNode;
import io.takamaka.code.engine.EngineClassLoader;
import io.takamaka.code.engine.InitialResponseBuilder;

/**
 * The creator of a response for a transaction that initializes a node.
 */
public class InitializationResponseBuilder extends InitialResponseBuilder<InitializationTransactionRequest, InitializationTransactionResponse> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public InitializationResponseBuilder(TransactionReference reference, InitializationTransactionRequest request, AbstractLocalNode<?,?> node) throws TransactionRejectedException {
		super(reference, request, node);
	}

	@Override
	public InitializationTransactionResponse getResponse() throws TransactionRejectedException {
		return new ResponseCreator() {

			@Override
			protected InitializationTransactionResponse body() {
				return new InitializationTransactionResponse();	
			}
		}
		.create();
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws Exception {
		return getCachedClassLoader(request.classpath); // currently not used for this transaction
	}
}