package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;

public class GameteCreationTransactionRun extends AbstractTransactionRun<GameteCreationTransactionRequest, GameteCreationTransactionResponse> {

	public GameteCreationTransactionRun(GameteCreationTransactionRequest request, TransactionReference current, Node node) throws TransactionException, IllegalTransactionRequestException {
		super(request, current, node, BigInteger.valueOf(-1L)); // we do not count gas for this creation
	}

	@Override
	protected EngineClassLoaderImpl mkClassLoader() throws Exception {
		return new EngineClassLoaderImpl(request.classpath, this);
	}

	@Override
	protected GameteCreationTransactionResponse computeResponse() throws Exception {
		if (request.initialAmount.signum() < 0)
			throw new IllegalTransactionRequestException("The gamete must be initialized with a non-negative amount of coins");

		// we create an initial gamete ExternallyOwnedContract and we fund it with the initial amount
		Object gamete = classLoader.getExternallyOwnedAccount().getDeclaredConstructor().newInstance();
		// we set the balance field of the gamete
		classLoader.setBalanceOf(gamete, request.initialAmount);

		return new GameteCreationTransactionResponse(updatesExtractor.extractUpdatesFrom(Stream.of(gamete)), classLoader.getStorageReferenceOf(gamete));
	}
}