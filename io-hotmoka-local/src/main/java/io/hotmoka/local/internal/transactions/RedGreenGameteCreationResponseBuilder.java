package io.hotmoka.local.internal.transactions;

import java.util.stream.Stream;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;
import io.hotmoka.local.EngineClassLoader;
import io.hotmoka.local.InitialResponseBuilder;
import io.hotmoka.local.internal.NodeInternal;

/**
 * The creator of a response for a transaction that creates a red/green gamete.
 */
public class RedGreenGameteCreationResponseBuilder extends InitialResponseBuilder<RedGreenGameteCreationTransactionRequest, GameteCreationTransactionResponse> {

	/**
	 * Creates the builder of a response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public RedGreenGameteCreationResponseBuilder(TransactionReference reference, RedGreenGameteCreationTransactionRequest request, NodeInternal node) throws TransactionRejectedException {
		super(reference, request, node);
	}

	@Override
	protected EngineClassLoader mkClassLoader() {
		return node.getCaches().getClassLoader(request.classpath);
	}

	@Override
	public GameteCreationTransactionResponse getResponse() throws TransactionRejectedException {
		return new ResponseCreator() {

			@Override
			protected GameteCreationTransactionResponse body() {
				try {
					Object gamete = classLoader.getRedGreenExternallyOwnedAccount().getDeclaredConstructor(String.class).newInstance(request.publicKey);
					classLoader.setBalanceOf(gamete, request.initialAmount);
					classLoader.setRedBalanceOf(gamete, request.redInitialAmount);
					return new GameteCreationTransactionResponse(updatesExtractor.extractUpdatesFrom(Stream.of(gamete)), classLoader.getStorageReferenceOf(gamete));
				}
				catch (Throwable t) {
					throw InternalFailureException.of(t);
				}
			}
		}
		.create();
	}
}