package io.takamaka.code.engine.internal.transactions;

import io.hotmoka.beans.TransactionException;
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
	 * The response computed at the end of the transaction.
	 */
	private final JarStoreInitialTransactionResponse response;

	/**
	 * Builds the creator of a transaction that installs a jar in the node, during its initialization.
	 * 
	 * @param request the request of the transaction
	 * @param current the reference that must be used for the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionException if the transaction cannot be created
	 */
	public JarStoreInitialTransactionBuilder(JarStoreInitialTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(current, node);

		byte[] jar = request.getJar();

		try {
			this.classLoader = new EngineClassLoader(jar, current, request.getDependencies(), this);
			InstrumentedJar instrumentedJar = InstrumentedJar.of(VerifiedJar.of(jar, classLoader, true), node.getGasCostModel());
			this.response = new JarStoreInitialTransactionResponse(instrumentedJar.toBytes());
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t);
		}
	}

	@Override
	public final JarStoreInitialTransactionResponse getResponse() {
		return response;
	}

	@Override
	public final EngineClassLoader getClassLoader() {
		return classLoader;
	}
}