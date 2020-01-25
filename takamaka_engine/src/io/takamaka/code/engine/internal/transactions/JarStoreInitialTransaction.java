package io.takamaka.code.engine.internal.transactions;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.takamaka.code.engine.Node;

public class JarStoreInitialTransaction extends AbstractTransaction<JarStoreInitialTransactionRequest, JarStoreInitialTransactionResponse> {

	public JarStoreInitialTransaction(JarStoreInitialTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node);
	}

	@Override
	protected JarStoreInitialTransactionResponse run(JarStoreInitialTransactionRequest request, TransactionReference current, Node node) throws TransactionException {
		return new JarStoreInitialTransactionRun(request, current, node).response;
	}
}