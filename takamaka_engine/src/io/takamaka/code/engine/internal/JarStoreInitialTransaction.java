package io.takamaka.code.engine.internal;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;

public class JarStoreInitialTransaction extends AbstractTransaction<JarStoreInitialTransactionResponse> {
	private final JarStoreInitialTransactionResponse response;

	public JarStoreInitialTransaction(JarStoreInitialTransactionRequest request, TransactionReference current) throws TransactionException {
		super(request);

		this.response = run(request, current);
	}

	@Override
	public JarStoreInitialTransactionResponse getResponse() {
		return response;
	}

	private JarStoreInitialTransactionResponse run(JarStoreInitialTransactionRequest request, TransactionReference current) throws TransactionException {
		return null; // TODO
	}
}