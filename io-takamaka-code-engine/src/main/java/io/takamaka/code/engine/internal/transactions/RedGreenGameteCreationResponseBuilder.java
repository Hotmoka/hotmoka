package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;
import java.util.concurrent.Callable;
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
	 * @param current the reference that must be used for the transaction
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
	public final GameteCreationTransactionResponse build(TransactionReference current) throws TransactionRejectedException {
		try {
			return new ResponseCreator(current).response;
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	private class ResponseCreator extends InitialResponseBuilder<RedGreenGameteCreationTransactionRequest, GameteCreationTransactionResponse>.ResponseCreator {
		
		/**
		 * The created response.
		 */
		private final GameteCreationTransactionResponse response;

		private ResponseCreator(TransactionReference current) throws Throwable {
			// we create an initial gamete RedGreenExternallyOwnedContract and we fund it with the initial amount
			GameteThread thread = new GameteThread(current);
			thread.go();
			Object gamete = thread.gamete;
			EngineClassLoader classLoader = getClassLoader();
			classLoader.setBalanceOf(gamete, request.initialAmount);
			classLoader.setRedBalanceOf(gamete, request.redInitialAmount);
			response = new GameteCreationTransactionResponse(updatesExtractor.extractUpdatesFrom(Stream.of(gamete)), classLoader.getStorageReferenceOf(gamete));
		}

		@Override
		public void event(Object event) {
		}

		@Override
		public <T> T withGas(BigInteger amount, Callable<T> what) throws Exception {
			// initial transactions consume no gas; this implementation is needed
			// if (in the future) code run in initial transactions tries to run
			// tasks with a limited amount of gas
			return what.call();
		}

		/**
		 * The thread that runs the code that creates the gamete.
		 */
		private class GameteThread extends TakamakaThread {
			private Object gamete;

			private GameteThread(TransactionReference current) throws Exception {
				super(current);
			}

			@Override
			protected void body() throws Exception {
				gamete = getClassLoader().getRedGreenExternallyOwnedAccount().getDeclaredConstructor().newInstance();
			}
		}
	}
}