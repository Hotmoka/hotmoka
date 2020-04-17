package io.takamaka.code.engine.internal.transactions;

import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.takamaka.code.engine.AbstractNode;
import io.takamaka.code.engine.internal.EngineClassLoader;

/**
 * The creator of a response for a transaction that creates a red/green gamete.
 */
public class RedGreenGameteCreationResponseBuilder extends InitialResponseBuilder<RedGreenGameteCreationTransactionRequest, GameteCreationTransactionResponse> {

	/**
	 * Creates the builder of a response.
	 * 
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public RedGreenGameteCreationResponseBuilder(RedGreenGameteCreationTransactionRequest request, AbstractNode node) throws TransactionRejectedException {
		super(request, node);

		try {
			if (request.initialAmount.signum() < 0 || request.redInitialAmount.signum() < 0)
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

	private class ResponseCreator extends InitialResponseBuilder<RedGreenGameteCreationTransactionRequest, GameteCreationTransactionResponse>.ResponseCreator {
		
		private ResponseCreator(TransactionReference current) throws TransactionRejectedException {
			super(current);
		}

		@Override
		protected GameteCreationTransactionResponse body() throws Exception {
			// we create an initial gamete RedGreenExternallyOwnedContract and we fund it with the initial amount
			Object gamete = classLoader.getRedGreenExternallyOwnedAccount().getDeclaredConstructor().newInstance();
			classLoader.setBalanceOf(gamete, request.initialAmount);
			classLoader.setRedBalanceOf(gamete, request.redInitialAmount);
			return new GameteCreationTransactionResponse(updatesExtractor.extractUpdatesFrom(Stream.of(gamete)), classLoader.getStorageReferenceOf(gamete));
		}
	}
}