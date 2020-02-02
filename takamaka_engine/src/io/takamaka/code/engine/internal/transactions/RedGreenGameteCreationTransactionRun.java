package io.takamaka.code.engine.internal.transactions;

import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.internal.EngineClassLoader;

public class RedGreenGameteCreationTransactionRun extends AbstractTransactionRun<RedGreenGameteCreationTransactionRequest, GameteCreationTransactionResponse> {
	private final EngineClassLoader classLoader;

	/**
	 * The response computed at the end of the transaction.
	 */
	private final GameteCreationTransactionResponse response;

	public RedGreenGameteCreationTransactionRun(RedGreenGameteCreationTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node);

		try (EngineClassLoader classLoader = new EngineClassLoader(request.classpath, this)) {
			this.classLoader = classLoader;

			if (request.initialAmount.signum() < 0 || request.redInitialAmount.signum() < 0)
				throw new IllegalArgumentException("the gamete must be initialized with a non-negative amount of coins");

			// we create an initial gamete RedGreenExternallyOwnedContract and we fund it with the initial amount
			Object gamete = classLoader.getRedGreenExternallyOwnedAccount().getDeclaredConstructor().newInstance();
			classLoader.setBalanceOf(gamete, request.initialAmount);
			classLoader.setRedBalanceOf(gamete, request.redInitialAmount);
			this.response = new GameteCreationTransactionResponse(updatesExtractor.extractUpdatesFrom(Stream.of(gamete)), classLoader.getStorageReferenceOf(gamete));
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t);
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
}