package io.hotmoka.nodes;

import java.util.Optional;

import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;

/**
 * An asynchronous node, that receives transactions, eventually runs them and adds
 * updates to the store of the node. The response of the transactions is available
 * as a future. For an asynchronous node, transactions have no time ordering.
 */
public interface AsynchronousNode extends Node {

	/**
	 * Posts a transaction that expands the store of this node with a transaction that installs a jar in it.
	 * If the transaction could not be completed successfully
	 * and the caller has been identified, the node store will still be expanded
	 * with a transaction that charges the gas limit to the caller, but no jar will be installed.
	 * Otherwise, the transaction will be rejected and not added to this node's store.
	 * 
	 * @param request the transaction request
	 */
	void postJarStoreTransaction(JarStoreTransactionRequest request);

	/**
	 * Posts a transaction that runs a constructor of a class in this node.
	 * If the transaction could not be completed successfully,
	 * for instance because of {@linkplain OutOfGasError}s and {@linkplain io.takamaka.code.lang.InsufficientFundsError}s,
	 * and the caller has been identified, the node's store will still be expanded
	 * with a transaction that charges all gas limit to the caller, but no constructor will be executed.
	 * Otherwise, the transaction will be rejected and not added to this node's store.
	 * If the constructor is annotated as {@linkplain io.takamaka.code.lang.ThrowsExceptions}
	 * and the constructor threw a {@linkplain CodeExecutionException} then,
	 * from the point of view of Takamaka, the transaction was successful, it gets added to this node's store
	 * and the consumed gas gets charged to the caller.
	 * 
	 * @param request the request of the transaction
	 */
	void postConstructorCallTransaction(ConstructorCallTransactionRequest request);

	/**
	 * Posts a transaction that runs an instance method of an object already in this node's store.
	 * If the transaction could not be completed successfully, also because of
	 * {@linkplain OutOfGasError}s and {@linkplain io.takamaka.code.lang.InsufficientFundsError}s,
	 * and the caller has been identified, the node's store will still be expanded
	 * with a transaction that charges all gas limit to the caller, but no method will be executed.
	 * Otherwise, the transaction will be rejected and not added to this node's store.
	 * If the method is annotated as {@linkplain io.takamaka.code.lang.ThrowsExceptions} and its execution
	 * failed with a checked exception then, from the point of view of Takamaka,
	 * the transaction was successful, it gets added to this node's store and the consumed gas gets charged to the caller.
	 * 
	 * @param request the transaction request
	 */
	void postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request);

	/**
	 * Posts a request that runs a static method of a class in this node.
	 * If the transaction could not be completed successfully, also because of
	 * {@linkplain OutOfGasError}s and {@linkplain io.takamaka.code.lang.InsufficientFundsError}s,
	 * and the caller has been identified, the node's store will still be expanded
	 * with a transaction that charges all gas limit to the caller, but no method will be executed.
	 * Otherwise, the transaction will be rejected and not added to this node's store.
	 * If the method is annotated as {@linkplain io.takamaka.code.lang.ThrowsExceptions} and its execution
	 * failed with a checked exception then, from the point of view of Takamaka,
	 * the transaction was successful, it gets added to this node's store and the consumed gas gets charged to the caller.
	 * 
	 * @param request the transaction request
	 */
	void postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request);

	/**
	 * Yields the response that was generated for the given request. If the request has been posted but
	 * not yet executed, then the optional result is empty.
	 * 
	 * @param request the request
	 * @return the response, if any
	 */
	<Request extends TransactionRequest<Response>, Response extends TransactionResponse> Optional<Response> peekResponseAt(Request request);
}