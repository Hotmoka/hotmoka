package io.takamaka.code.engine.internal.transactions;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.takamaka.code.engine.AbstractLocalNode;
import io.takamaka.code.engine.EngineClassLoader;
import io.takamaka.code.engine.InitialResponseBuilder;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.VerifiedJar;

/**
 * Builds the creator of response for a transaction that installs a jar in the node, during its initialization.
 */
public class JarStoreInitialResponseBuilder extends InitialResponseBuilder<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public JarStoreInitialResponseBuilder(TransactionReference reference, JarStoreInitialTransactionRequest request, AbstractLocalNode<?,?> node) throws TransactionRejectedException {
		super(reference, request, node);
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws Exception {
		return new EngineClassLoader(request.getJar(), request.getDependencies(), node);
	}

	@Override
	public JarStoreInitialTransactionResponse getResponse() throws TransactionRejectedException {
		return new ResponseCreator() {

			@Override
			protected JarStoreInitialTransactionResponse body() {
				try {
					InstrumentedJar instrumentedJar = InstrumentedJar.of(VerifiedJar.of(request.getJar(), classLoader, true), node.getGasCostModel());
					return new JarStoreInitialTransactionResponse(instrumentedJar.toBytes(), request.getDependencies());
				}
				catch (Throwable t) {
					throw InternalFailureException.of(t);
				}
			}
		}
		.create();
	}
}