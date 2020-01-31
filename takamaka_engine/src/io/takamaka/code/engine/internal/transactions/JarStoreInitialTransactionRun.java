package io.takamaka.code.engine.internal.transactions;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.nodes.Node;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.VerifiedJar;

public class JarStoreInitialTransactionRun extends AbstractTransactionRun<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> {
	public JarStoreInitialTransactionRun(JarStoreInitialTransactionRequest request, TransactionReference current, Node node) throws TransactionException, IllegalTransactionRequestException {
		super(request, current, node);

		try (EngineClassLoaderImpl classLoader = new EngineClassLoaderImpl(request.getJar(), request.getDependencies(), this)) {
			this.classLoader = classLoader;
			this.response = computeResponse();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "cannot complete the transaction");
		}
	}

	private JarStoreInitialTransactionResponse computeResponse() throws Exception {
		VerifiedJar verifiedJar = VerifiedJar.of(classLoader.jarPath(), classLoader, true);
		InstrumentedJar instrumentedJar = InstrumentedJar.of(verifiedJar, new GasCostModelAdapter(node.getGasCostModel()));
		return new JarStoreInitialTransactionResponse(instrumentedJar.toBytes());
	}
}