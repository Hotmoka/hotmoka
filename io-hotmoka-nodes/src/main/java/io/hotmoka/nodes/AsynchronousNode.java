package io.hotmoka.nodes;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

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
	 * @return the future holding the reference to the transaction where the jar has been installed
	 * @throws TransactionException if an error prevented the transaction from being posted
	 */
	JarStoreFuture postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionException ;

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
	 * @return the future holding the result of the computation
	 * @throws TransactionException if an error prevented the transaction from being posted
	 */
	CodeExecutionFuture<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionException;

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
	 * @return the future holding the result of the transaction
	 * @throws TransactionException if an error prevented the transaction from being posted
	 */
	CodeExecutionFuture<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionException;

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
	 * @throws TransactionException if an error prevented the transaction from being posted
	 */
	void postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionException;

	/**
	 * The future of a transaction that executes code in blockchain.
	 * 
	 * @param <V> the type of the value computed by the transaction
	 */
	interface CodeExecutionFuture<V extends StorageValue> {
	
		/**
	     * Waits if necessary for the transaction to complete, and then retrieves its result.
	     *
	     * @return the computed result of the transaction
	     * @throws TransactionException if the transaction could not be completed successfully. This includes
	     *                              {@link io.hotmoka.nodes.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s.
	     *                              In this case, the node's store will still be expanded
	     *                              with a transaction that charges all gas limit to the caller, but no code will be executed
	     * @throws CodeExecutionException if the method or constructor is annotated as {@link io.takamaka.code.lang.ThrowsExceptions} and its execution
	     *                                failed with a checked exception. Note that, in this case, from the point of view of Takamaka,
	     *                                the transaction was successful, it gets added to this node's store and the consumed gas gets charged to the caller.
	     *                                In all other cases, a {@link io.hotmoka.beans.TransactionException} is thrown
	     */
	    V get() throws TransactionException, CodeExecutionException;
	
	    /**
	     * Waits if necessary for at most the given time for the transaction
	     * to complete, and then retrieves its result, if available.
	     *
	     * @param timeout the maximum time to wait
	     * @param unit the time unit of the timeout argument
	     * @return the computed result of the transaction
	     * @throws TransactionException if the transaction could not be completed successfully. This includes
	     *                              {@link io.hotmoka.nodes.OutOfGasError}s and {@link io.takamaka.code.lang.InsufficientFundsError}s.
	     *                              In this case, the node's store will still be expanded
	     *                              with a transaction that charges all gas limit to the caller, but no code will be executed
	     * @throws CodeExecutionException if the method or constructor is annotated as {@link io.takamaka.code.lang.ThrowsExceptions} and its execution
	     *                                failed with a checked exception. Note that, in this case, from the point of view of Takamaka,
	     *                                the transaction was successful, it gets added to this node's store and the consumed gas gets charged to the caller.
	     *                                In all other cases, a {@link io.hotmoka.beans.TransactionException} is thrown
	     * @throws TimeoutException if the timeout expired but the result has not been computed yet
	     */
	    V get(long timeout, TimeUnit unit) throws TransactionException, CodeExecutionException, TimeoutException;

	    /**
	     * Yields an identifier of the transaction, that can be used for polling its result.
	     * This can be, for instance, a hash of the transaction.
	     * 
	     * @return the identifier
	     */
	    String id();
	}

	/**
	 * The future of a transaction that stores a jar in blockchain.
	 */
	interface JarStoreFuture {

		/**
	     * Waits if necessary for the transaction to complete, and then retrieves its result.
	     *
	     * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
	     * @throws TransactionException if the transaction threw that exception
	     */
	    TransactionReference get() throws TransactionException;

	    /**
	     * Waits if necessary for at most the given time for the transaction
	     * to complete, and then retrieves its result, if available.
	     *
	     * @param timeout the maximum time to wait
	     * @param unit the time unit of the timeout argument
	     * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
	     * @throws TransactionException if the transaction threw that exception
	     * @throws TimeoutException if the timeout expired but the result has not been computed yet
	     */
	    TransactionReference get(long timeout, TimeUnit unit) throws TransactionException, TimeoutException;

	    /**
	     * Yields an identifier of the transaction, that can be used for polling its result.
	     * This can be, for instance, a hash of the transaction.
	     * 
	     * @return the identifier
	     */
	    String id();
	}
}