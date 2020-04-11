package io.takamaka.code.engine.internal.transactions;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.internal.EngineClassLoader;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.VerifiedJar;

/**
 * Builds the creator of a transaction that installs a jar in the node, during its initialization.
 */
public class JarStoreInitialTransactionBuilder extends InitialTransactionBuilder<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> {

	/**
	 * The class loader of the transaction.
	 */
	private final EngineClassLoader classLoader;

	/**
	 * The jar to install, as a byte array.
	 */
	private final byte[] jar;

	/**
	 * Creates the builder of a transaction that installs a jar in the node, during its initialization.
	 * 
	 * @param request the request of the transaction
	 * @param current the reference that must be used for the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public JarStoreInitialTransactionBuilder(JarStoreInitialTransactionRequest request, TransactionReference current, Node node) throws TransactionRejectedException {
		super(request, current, node);

		try {
			this.jar = request.getJar();
			this.classLoader = new EngineClassLoader(jar, current, request.getDependencies(), this);
		}
		catch (Throwable t) {
			throw wrapAsTransactionRejectedException(t);
		}
	}

	@Override
	public JarStoreInitialTransactionResponse build() throws TransactionRejectedException {
		try {
			InstrumentedJar instrumentedJar = InstrumentedJar.of(VerifiedJar.of(jar, classLoader, true), node.getGasCostModel());
			return new JarStoreInitialTransactionResponse(instrumentedJar.toBytes());
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