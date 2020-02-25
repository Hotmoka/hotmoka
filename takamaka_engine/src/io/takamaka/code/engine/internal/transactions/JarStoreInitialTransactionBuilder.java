package io.takamaka.code.engine.internal.transactions;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.internal.EngineClassLoader;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.VerifiedJar;

public class JarStoreInitialTransactionBuilder extends AbstractTransactionBuilder<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> {
	private final EngineClassLoader classLoader;

	/**
	 * The response computed at the end of the transaction.
	 */
	private final JarStoreInitialTransactionResponse response;

	public JarStoreInitialTransactionBuilder(JarStoreInitialTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(current, node);

		try (EngineClassLoader classLoader = new EngineClassLoader(request.getJar(), request.getDependencies(), this)) {
			this.classLoader = classLoader;
			VerifiedJar verifiedJar = VerifiedJar.of(classLoader.jarPath(), classLoader, true);
			InstrumentedJar instrumentedJar = InstrumentedJar.of(verifiedJar, node.getGasCostModel());
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