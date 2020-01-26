package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.takamaka.code.engine.Node;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;
import io.takamaka.code.engine.internal.TempJarFile;
import io.takamaka.code.instrumentation.InstrumentedJar;
import io.takamaka.code.verification.VerifiedJar;

public class JarStoreInitialTransactionRun extends AbstractTransactionRun<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> {

	public JarStoreInitialTransactionRun(JarStoreInitialTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node, BigInteger.valueOf(-1L)); // we do not count gas for this creation
	}

	@Override
	protected JarStoreInitialTransactionResponse computeResponse() throws Exception {
		// we transform the array of bytes into a real jar file
		try (TempJarFile original = new TempJarFile(request.getJar());
			EngineClassLoaderImpl jarClassLoader = new EngineClassLoaderImpl(original.toPath(), request.getDependencies(), this)) {
			VerifiedJar verifiedJar = VerifiedJar.of(original.toPath(), jarClassLoader, true);
			InstrumentedJar instrumentedJar = InstrumentedJar.of(verifiedJar, gasModelAsForInstrumentation());
			return new JarStoreInitialTransactionResponse(instrumentedJar.toBytes());
		}
	}
}