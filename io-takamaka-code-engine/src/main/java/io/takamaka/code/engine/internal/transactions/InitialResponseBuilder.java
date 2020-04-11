package io.takamaka.code.engine.internal.transactions;

import java.math.BigInteger;
import java.util.concurrent.Callable;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InitialTransactionRequest;
import io.hotmoka.beans.responses.InitialTransactionResponse;
import io.hotmoka.nodes.Node;

/**
 * The creator of the response for an initial transaction. Initial transactions do not consume gas.
 */
public abstract class InitialResponseBuilder<Request extends InitialTransactionRequest<Response>, Response extends InitialTransactionResponse> extends AbstractResponseBuilder<Request, Response> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param request the request of the transaction
	 * @param current the reference that must be used to refer to the transaction
	 * @param node the node that is creating the response
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	protected InitialResponseBuilder(Request request, TransactionReference current, Node node) throws TransactionRejectedException {
		super(request, current, node);
	}

	@Override
	public final void chargeGasForCPU(BigInteger amount) {
		// initial transactions consume no gas; this implementation is needed
		// since code run in initial transactions (such as the creation of gametes)
		// tries to charge for gas
	}

	@Override
	public final void chargeGasForRAM(BigInteger amount) {
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