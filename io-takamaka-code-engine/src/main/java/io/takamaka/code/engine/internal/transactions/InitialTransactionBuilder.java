package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;
import java.util.concurrent.Callable;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InitialTransactionRequest;
import io.hotmoka.beans.responses.InitialTransactionResponse;
import io.hotmoka.nodes.Node;

/**
 * The creator of an initial transaction. Initial transactions do not consume gas.
 */
public abstract class InitialTransactionBuilder<Request extends InitialTransactionRequest<Response>, Response extends InitialTransactionResponse> extends AbstractTransactionBuilder<Request, Response> {

	/**
	 * Creates an initial transaction builder.
	 * 
	 * @param current the reference that must be used to refer to the created transaction
	 * @param node the node that is creating the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	protected InitialTransactionBuilder(TransactionReference current, Node node) throws TransactionRejectedException {
		super(current, node);
	}

	@Override
	public final void chargeForCPU(BigInteger amount) {
		// initial transactions consume no gas; this implementation is needed
		// since code run in initial transactions (such as the creation of gametes)
		// tries to charge for gas
	}

	@Override
	public final void chargeForRAM(BigInteger amount) {
		// initial transactions consume no gas; this implementation is needed
		// since code run in initial transactions (such as the creation of gametes)
		// tries to charge for gas
	}

	@Override
	public final <T> T withGas(BigInteger amount, Callable<T> what) throws Exception {
		// initial transactions consume no gas; this implementation is needed
		// if (in the future) code run in initial transactions tries to run
		// tasks with a limited amount of gas
		return what.call();
	}
}