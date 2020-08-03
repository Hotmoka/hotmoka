package io.takamaka.code.engine.internal.transactions;

import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.takamaka.code.engine.AbstractNode;
import io.takamaka.code.engine.EngineClassLoader;
import io.takamaka.code.engine.InitialResponseBuilder;

/**
 * The creator of a response for a transaction that creates a gamete.
 */
public class GameteCreationResponseBuilder extends InitialResponseBuilder<GameteCreationTransactionRequest, GameteCreationTransactionResponse> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public GameteCreationResponseBuilder(TransactionReference reference, GameteCreationTransactionRequest request, AbstractNode<?,?> node) throws TransactionRejectedException {
		super(reference, request, node);
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws Exception {
		return node.getCachedClassLoader(request.classpath);
	}

	@Override
	public GameteCreationTransactionResponse build() throws TransactionRejectedException {
		return new ResponseCreator() {

			@Override
			protected GameteCreationTransactionResponse body() throws Exception {
				if (isInitializedUncommitted())
					throw new TransactionRejectedException("cannot run a " + GameteCreationTransactionRequest.class.getSimpleName() + " in an already initialized node");

				// we create an initial gamete ExternallyOwnedContract and we fund it with the initial amount
				Object gamete = classLoader.getExternallyOwnedAccount().getConstructor(String.class).newInstance(request.publicKey);
				classLoader.setBalanceOf(gamete, request.initialAmount);
				return new GameteCreationTransactionResponse(updatesExtractor.extractUpdatesFrom(Stream.of(gamete)), classLoader.getStorageReferenceOf(gamete));	
			}
		}
		.create();
	}
}