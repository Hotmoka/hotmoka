package io.takamaka.code.engine.internal.transactions;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.takamaka.code.engine.AbstractNode;
import io.takamaka.code.engine.internal.EngineClassLoader;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.VerifiedJar;

/**
 * Builds the creator of response for a transaction that installs a jar in the node, during its initialization.
 */
public class JarStoreInitialResponseBuilder extends InitialResponseBuilder<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public JarStoreInitialResponseBuilder(JarStoreInitialTransactionRequest request, AbstractNode node) throws TransactionRejectedException {
		super(request, node);
	}

	@Override
	protected EngineClassLoader mkClassLoader() throws Exception {
		return new EngineClassLoader(request.getJar(), request.getDependencies(), node);
	}

	@Override
	public JarStoreInitialTransactionResponse build(TransactionReference current) throws TransactionRejectedException {
		return this.new ResponseCreator(current) {

			@Override
			protected JarStoreInitialTransactionResponse body() throws Exception {
				InstrumentedJar instrumentedJar = InstrumentedJar.of(VerifiedJar.of(request.getJar(), classLoader, true), node.getGasCostModel());
				return new JarStoreInitialTransactionResponse(instrumentedJar.toBytes(), request.getDependencies());
			}
		}
		.create();
	}
}