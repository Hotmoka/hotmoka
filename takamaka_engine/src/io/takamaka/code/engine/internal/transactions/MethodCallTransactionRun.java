package io.takamaka.code.engine.internal.transactions;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.MethodCallTransactionRequest;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.nodes.Node;

/**
 * A generic implementation of a blockchain. Specific implementations can subclass this class
 * and just implement the abstract template methods. The rest of code should work instead
 * as a generic layer for all blockchain implementations.
 */
public abstract class MethodCallTransactionRun<Request extends MethodCallTransactionRequest> extends CodeCallTransactionRun<Request, MethodCallTransactionResponse> {

	/**
	 * The method that is being called.
	 */
	public final MethodSignature method;

	/**
	 * True if the method has been called correctly and it is declared as {@code void},
	 */
	public boolean isVoidMethod;

	/**
	 * True if the method has been called correctly and it is annotated as {@link io.takamaka.code.lang.View}.
	 */
	public boolean isViewMethod;

	protected MethodCallTransactionRun(Request request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node);

		this.method = request.method;
	}


	@Override
	public final MethodSignature getMethodOrConstructor() {
		return method;
	}
}