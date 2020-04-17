package io.takamaka.code.engine.internal.transactions;

import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.takamaka.code.engine.AbstractNode;
import io.takamaka.code.engine.internal.EngineClassLoader;

/**
 * The creator of a response for a transaction that creates a gamete.
 */
public class GameteCreationResponseBuilder extends InitialResponseBuilder<GameteCreationTransactionRequest, GameteCreationTransactionResponse> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public GameteCreationResponseBuilder(GameteCreationTransactionRequest request, AbstractNode node) throws TransactionRejectedException {
		super(request, node);

		try {
			if (request.initialAmount.signum() < 0)
				throw new TransactionRejectedException("the gamete must be initialized with a non-negative amount of coins");
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws Exception {
		return node.getCachedClassLoader(request.classpath);
	}

	@Override
	public GameteCreationTransactionResponse build(TransactionReference current) throws TransactionRejectedException {
		return new ResponseCreator(current).create();
	}

	private class ResponseCreator extends InitialResponseBuilder<GameteCreationTransactionRequest, GameteCreationTransactionResponse>.ResponseCreator {
		
		private ResponseCreator(TransactionReference current) throws TransactionRejectedException {
			super(current);
		}

		@Override
		protected GameteCreationTransactionResponse body() throws Exception {
			// we create an initial gamete ExternallyOwnedContract and we fund it with the initial amount
			Object gamete = classLoader.getExternallyOwnedAccount().getDeclaredConstructor().newInstance();
			classLoader.setBalanceOf(gamete, request.initialAmount);
			return new GameteCreationTransactionResponse(updatesExtractor.extractUpdatesFrom(Stream.of(gamete)), classLoader.getStorageReferenceOf(gamete));			
		}
	}
}