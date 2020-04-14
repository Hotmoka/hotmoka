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
	 * The class loader of the transaction.
	 */
	private final EngineClassLoader classLoader;

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
			this.classLoader = node.getCachedClassLoader(request.classpath);

			if (request.initialAmount.signum() < 0)
				throw new IllegalArgumentException("the gamete must be initialized with a non-negative amount of coins");
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	public GameteCreationTransactionResponse build(TransactionReference current) throws TransactionRejectedException {
		try {
			// we create an initial gamete ExternallyOwnedContract and we fund it with the initial amount
			GameteThread thread = new GameteThread(current);
			thread.go();
			Object gamete = thread.gamete;
			classLoader.setBalanceOf(gamete, request.initialAmount);
			return new GameteCreationTransactionResponse(updatesExtractor.extractUpdatesFrom(Stream.of(gamete)), classLoader.getStorageReferenceOf(gamete));
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	public final EngineClassLoader getClassLoader() {
		return classLoader;
	}

	/**
	 * The thread that runs the code that creates the gamete.
	 */
	private class GameteThread extends TakamakaThread {
		private Object gamete;

		private GameteThread(TransactionReference current) {
			super(current);
		}

		@Override
		protected void body() throws Exception {
			gamete = classLoader.getExternallyOwnedAccount().getDeclaredConstructor().newInstance();
		}
	}
}