package io.takamaka.code.engine.internal.transactions;

import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.internal.EngineClassLoader;

/**
 * The creator of a response for a transaction that creates a red/green gamete.
 */
public class RedGreenGameteCreationResponseBuilder extends InitialResponseBuilder<RedGreenGameteCreationTransactionRequest, GameteCreationTransactionResponse> {

	/**
	 * The class loader of the transaction.
	 */
	private final EngineClassLoader classLoader;

	/**
	 * Creates the builder of a response.
	 * 
	 * @param request the request of the transaction
	 * @param current the reference that must be used for the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public RedGreenGameteCreationResponseBuilder(RedGreenGameteCreationTransactionRequest request, TransactionReference current, Node node) throws TransactionRejectedException {
		super(request, current, node);

		try {
			this.classLoader = new EngineClassLoader(request.classpath, this);

			if (request.initialAmount.signum() < 0 || request.redInitialAmount.signum() < 0)
				throw new IllegalArgumentException("the gamete must be initialized with a non-negative amount of coins");
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	public final GameteCreationTransactionResponse build() throws TransactionRejectedException {
		try {
			// we create an initial gamete RedGreenExternallyOwnedContract and we fund it with the initial amount
			GameteThread thread = new GameteThread();
			thread.go();
			Object gamete = thread.gamete;
			classLoader.setBalanceOf(gamete, request.initialAmount);
			classLoader.setRedBalanceOf(gamete, request.redInitialAmount);
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

		private GameteThread() {}

		@Override
		protected void body() throws Exception {
			gamete = classLoader.getRedGreenExternallyOwnedAccount().getDeclaredConstructor().newInstance();
		}
	}
}