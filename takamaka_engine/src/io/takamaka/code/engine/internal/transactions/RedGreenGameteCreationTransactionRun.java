package io.takamaka.code.engine.internal.transactions;

import java.lang.reflect.Field;
import java.math.BigInteger;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;

public class RedGreenGameteCreationTransactionRun extends AbstractTransactionRun<RedGreenGameteCreationTransactionRequest, GameteCreationTransactionResponse> {

	public RedGreenGameteCreationTransactionRun(RedGreenGameteCreationTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node, BigInteger.valueOf(-1L)); // we do not count gas for this creation
	}

	@Override
	protected GameteCreationTransactionResponse computeResponse() throws Exception {
		if (request.initialAmount.signum() < 0 || request.redInitialAmount.signum() < 0)
			throw new IllegalTransactionRequestException("The gamete must be initialized with a non-negative amount of coins");

		try (EngineClassLoaderImpl classLoader = this.classLoader = new EngineClassLoaderImpl(request.classpath, this)) {
			// we create an initial gamete RedGreenExternallyOwnedContract and we fund it with the initial amount
			Object gamete = classLoader.getRedGreenExternallyOwnedAccount().getDeclaredConstructor().newInstance();
			// we set the balance field of the gamete
			Field balanceField = classLoader.getContract().getDeclaredField("balance");
			balanceField.setAccessible(true); // since the field is private
			balanceField.set(gamete, request.initialAmount);

			// we set the red balance field of the gamete
			Field redBalanceField = classLoader.getRedGreenContract().getDeclaredField("balanceRed");
			redBalanceField.setAccessible(true); // since the field is private
			redBalanceField.set(gamete, request.redInitialAmount);

			return new GameteCreationTransactionResponse(collectUpdates(null, null, null, gamete).stream(), classLoader.getStorageReferenceOf(gamete));
		}
	}
}