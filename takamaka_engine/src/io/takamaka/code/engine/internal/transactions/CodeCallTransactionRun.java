package io.takamaka.code.engine.internal.transactions;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.CodeExecutionTransactionRequest;
import io.hotmoka.beans.responses.CodeExecutionTransactionResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.hotmoka.nodes.Node;

/**
 * A generic implementation of a blockchain. Specific implementations can subclass this class
 * and just implement the abstract template methods. The rest of code should work instead
 * as a generic layer for all blockchain implementations.
 */
public abstract class CodeCallTransactionRun<Request extends CodeExecutionTransactionRequest<Response>, Response extends CodeExecutionTransactionResponse> extends NonInitialTransactionRun<Request, Response> {

	public UpdateOfBalance balanceUpdateInCaseOfFailure;

	/**
	 * The deserialized caller.
	 */
	public Object deserializedCaller;

	/**
	 * The deserialized actual arguments of the call.
	 */
	public Object[] deserializedActuals;

	/**
	 * The resulting value for methods or the created object for constructors.
	 * This is {@code null} if the execution completed with an exception or
	 * if the method actually returned {@code null}.
	 */
	public Object result;

	/**
	 * The exception resulting from the execution of the method or constructor, if any.
	 * This is {@code null} if the execution completed without exception.
	 */
	public Throwable exception;

	protected CodeCallTransactionRun(Request request, TransactionReference current, Node node) throws TransactionException {
		super(request, current, node);
	}

	/**
	 * Yields the method or constructor that is being called.
	 * 
	 * @return the method or constructor that is being called
	 */
	public abstract CodeSignature getMethodOrConstructor();
}