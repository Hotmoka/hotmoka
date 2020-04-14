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
	 * The class loader of the transaction.
	 */
	private final EngineClassLoader classLoader;

	/**
	 * The jar to install, as a byte array.
	 */
	private final byte[] jar;

	/**
	 * Creates the builder of the response.
	 * 
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public JarStoreInitialResponseBuilder(JarStoreInitialTransactionRequest request, AbstractNode node) throws TransactionRejectedException {
		super(request, node);

		try {
			this.jar = request.getJar();
			this.classLoader = new EngineClassLoader(jar, request.getDependencies(), node);
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	public JarStoreInitialTransactionResponse build(TransactionReference current) throws TransactionRejectedException {
		try {
			InstrumentedJar instrumentedJar = InstrumentedJar.of(VerifiedJar.of(jar, classLoader, true), node.getGasCostModel());
			return new JarStoreInitialTransactionResponse(instrumentedJar.toBytes(), request.getDependencies());
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	public final EngineClassLoader getClassLoader() {
		return classLoader;
	}
}