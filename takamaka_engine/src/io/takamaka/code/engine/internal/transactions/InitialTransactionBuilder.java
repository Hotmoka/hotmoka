package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;
import java.util.concurrent.Callable;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InitialTransactionRequest;
import io.hotmoka.beans.responses.InitialTransactionResponse;
import io.hotmoka.nodes.Node;

/**
 * A generic implementation of a blockchain. Specific implementations can subclass this class
 * and just implement the abstract template methods. The rest of code should work instead
 * as a generic layer for all blockchain implementations.
 */
public abstract class InitialTransactionBuilder<Request extends InitialTransactionRequest<Response>, Response extends InitialTransactionResponse> extends AbstractTransactionBuilder<Request, Response> {

	protected InitialTransactionBuilder(TransactionReference current, Node node) throws TransactionException {
		super(current, node);
	}

	@Override
	public final void chargeForCPU(BigInteger amount) {}

	@Override
	public final void chargeForRAM(BigInteger amount) {}

	@Override
	public final <T> T withGas(BigInteger amount, Callable<T> what) throws Exception {
		return what.call();
	}
}