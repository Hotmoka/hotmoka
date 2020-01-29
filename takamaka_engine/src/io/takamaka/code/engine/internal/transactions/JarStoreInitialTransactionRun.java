package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;

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
		super(request, current, node, BigInteger.valueOf(-1L)); // we do not count gas for this creation
	}

	@Override
	protected EngineClassLoaderImpl mkClassLoader() throws Exception {
		return new EngineClassLoaderImpl(request.getJar(), request.getDependencies(), this);
	}

	@Override
	protected JarStoreInitialTransactionResponse computeResponse() throws Exception {
		VerifiedJar verifiedJar = VerifiedJar.of(classLoader.jarPath(), classLoader, true);
		InstrumentedJar instrumentedJar = InstrumentedJar.of(verifiedJar, new GasCostModelAdapter(node.getGasCostModel()));
		return new JarStoreInitialTransactionResponse(instrumentedJar.toBytes());
	}
}