package io.takamaka.code.engine.internal.transactions;

import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.internal.EngineClassLoader;

/**
 * The creator of a transaction that creates a red/green gamete.
 */
public class RedGreenGameteCreationTransactionBuilder extends InitialTransactionBuilder<RedGreenGameteCreationTransactionRequest, GameteCreationTransactionResponse> {

	/**
	 * The class loader of the transaction.
	 */
	private final EngineClassLoader classLoader;

	/**
	 * The response computed at the end of the transaction.
	 */
	private final GameteCreationTransactionResponse response;

	/**
	 * Creates the builder of a transaction that creates a red/green gamete.
	 * 
	 * @param request the request of the transaction
	 * @param current the reference that must be used for the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the transaction cannot be created
	 */
	public RedGreenGameteCreationTransactionBuilder(RedGreenGameteCreationTransactionRequest request, TransactionReference current, Node node) throws TransactionRejectedException {
		super(request, current, node);

		try {
			this.classLoader = new EngineClassLoader(request.classpath, this);

			if (request.initialAmount.signum() < 0 || request.redInitialAmount.signum() < 0)
				throw new IllegalArgumentException("the gamete must be initialized with a non-negative amount of coins");

			// we create an initial gamete RedGreenExternallyOwnedContract and we fund it with the initial amount
			GameteThread thread = new GameteThread();
			thread.start();
			thread.join();
			if (thread.exception != null)
				throw thread.exception;

			classLoader.setBalanceOf(thread.gamete, request.initialAmount);
			classLoader.setRedBalanceOf(thread.gamete, request.redInitialAmount);
			this.response = new GameteCreationTransactionResponse(updatesExtractor.extractUpdatesFrom(Stream.of(thread.gamete)), classLoader.getStorageReferenceOf(thread.gamete));
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	public final EngineClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public final GameteCreationTransactionResponse getResponse() {
		return response;
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