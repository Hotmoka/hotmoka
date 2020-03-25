package io.hotmoka.nodes;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A synchronous node, that executes transactions, adds
 * updates to the store of the node and yields the response of the transactions.
 * For a synchronous node, transactions have a time ordering,
 * so that it is possible to know which has been added before in the node.
 */
public interface SynchronousNode extends Node {

	/**
	 * Expands the store of this node with a transaction that installs a jar in it.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
	 * @throws TransactionException if the transaction could not be completed successfully. If this occurs and the caller
	 *                              has been identified, the node store will still be expanded
	 *                              with a transaction that charges the gas limit to the caller, but no jar will be installed.
	 *                              Otherwise, the transaction will be rejected and not added to this node's store
	 */
	TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionException;

	/**
	 * Expands this node's store with a transaction that runs a constructor of a class.
	 * 
	 * @param request the request of the transaction
	 * @return the created object, if the constructor was successfully executed, without exception
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                              {@link io.hotmoka.nodes.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s.
	 *                              If this occurs and the caller has been identified, the node's store will still be expanded
	 *                              with a transaction that charges all gas limit to the caller, but no constructor will be executed.
	 *                              Otherwise, the transaction will be rejected and not added to this node's store
	 * @throws CodeExecutionException if the constructor is annotated as {@link io.takamaka.code.lang.ThrowsExceptions} and its execution
	 *                                failed with a checked exception. Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it gets added to this node's store and the consumed gas gets charged to the caller.
	 *                                In all other cases, a {@link io.hotmoka.beans.TransactionException} is thrown
	 */
	StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionException, CodeExecutionException;

	/**
	 * Expands this node's store with a transaction that runs an instance method of an object already in this node's store.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                              {@link io.hotmoka.nodes.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s.
	 *                              If this occurs and the caller has been identified, the node's store will still be expanded
	 *                              with a transaction that charges all gas to the caller, but no method will be executed.
	 *                              Otherwise, the transaction will be rejected and not added to this node's store
	 * @throws CodeExecutionException if the method is annotated as {@link io.takamaka.code.lang.ThrowsExceptions} and its execution
	 *                                failed with a checked exception. Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it gets added to this node's store and the consumed gas gets charged to the caller.
	 *                                In all other cases, a {@link io.hotmoka.beans.TransactionException} is thrown
	 */
	StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException;

	/**
	 * Expands this node's store with a transaction that runs a static method of a class in this node.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                              {@link io.hotmoka.nodes.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s.
	 *                              If this occurs and the caller has been identified, the node's store will still be expanded
	 *                              with a transaction that charges all gas limit to the caller, but no method will be executed.
	 *                              Otherwise, the transaction will be rejected and not added to this node's store
	 * @throws CodeExecutionException if the method is annotated as {@link io.takamaka.code.lang.ThrowsExceptions} and its execution
	 *                                failed with a checked exception. Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it gets added to this node's store and the consumed gas gets charged to the caller.
	 *                                In all other cases, a {@link io.hotmoka.beans.TransactionException} is thrown
	 */
	StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException;
}