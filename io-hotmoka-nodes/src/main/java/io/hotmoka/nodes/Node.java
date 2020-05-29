package io.hotmoka.nodes;

import java.util.NoSuchElementException;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A node of the Hotmoka network, that provides the storage
 * facilities for the execution of Takamaka code.
 * Calls to code in the node can be added, run or posted.
 * Posted calls are executed, eventually, and their value can be retrieved
 * through the future returned by the calls. Added calls are shorthand
 * for posting a call and waiting until the value of their future is
 * available. Run calls are only available for view methods, without side-effects.
 * They execute immediately and never modify the store of the node.
 */
public interface Node extends AutoCloseable {

	/**
	 * Yields the reference, in the store of the node, where the base Takamaka base classes are installed.
	 */
	TransactionReference takamakaCode();

	/**
	 * Yields the manifest installed in the store of the node. The manifest is an object of type
	 * {@code io.takamaka.code.system.Manifest} that contains some information about the node,
	 * useful for the users of the node.
	 * 
	 * @return the reference to the node
	 * @throws NoSuchElementException if no manifest has been set for this node
	 */
	StorageReference manifest() throws NoSuchElementException;

	/**
	 * Yields the class tag of the object with the given storage reference.
	 * 
	 * @param reference the storage reference
	 * @return the class tag, if any
	 * @throws NoSuchElementException if the class tag could not be found
	 */
	ClassTag getClassTag(StorageReference reference) throws NoSuchElementException;

	/**
	 * Yields the current state of the object at the given storage reference.
	 * If this method succeeds and this node has some form of commit, then the transaction
	 * of the storage reference has been definitely committed in this node.
	 * 
	 * @param reference the storage reference of the object
	 * @return the last updates of all its instance fields; these updates include
	 *         the class tag update for the object
	 * @throws NoSuchElementException if there is no object with that reference
	 */
	Stream<Update> getState(StorageReference reference) throws NoSuchElementException;

	/**
	 * Yields the UTC time that must be used for a transaction, if it is executed
	 * with this node in this moment.
	 * 
	 * @return the UTC time, as returned by {@link java.lang.System#currentTimeMillis()}
	 */
	long getNow();

	/**
	 * Expands the store of this node with a transaction that
	 * installs a jar in it. It has no caller and requires no gas. The goal is to install, in the
	 * node, some basic jars that are likely needed as dependencies by future jars.
	 * For instance, the jar containing the basic contract classes.
	 * This installation have special privileges, such as that of installing
	 * packages in {@code io.takamaka.code.lang.*}.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 */
	TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException;

	/**
	 * Expands the store of this node with a transaction that creates a gamete, that is,
	 * an externally owned contract with the given initial amount of coins.
	 * This transaction has no caller and requires no gas.
	 * 
	 * @param request the transaction request
	 * @return the reference to the freshly created gamete
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 */
	StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException;

	/**
	 * Expands the store of this node with a transaction that creates a red/green gamete, that is,
	 * a red/green externally owned contract with the given initial amount of coins.
	 * This transaction has no caller and requires no gas.
	 * 
	 * @param request the transaction request
	 * @return the reference to the freshly created gamete
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 */
	StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException;

	/**
	 * Expands the store of this node with a transaction that marks the node as
	 * initialized and installs its manifest. After this transaction, no more initial transactions
	 * can be executed on the node.
	 * 
	 * @param request the transaction request
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 */
	void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException;

	/**
	 * Expands the store of this node with a transaction that installs a jar in it.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws TransactionException if the transaction could not be executed and the store of the node has been expanded with a failed transaction
	 */
	TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException;

	/**
	 * Expands this node's store with a transaction that runs a constructor of a class.
	 * 
	 * @param request the request of the transaction
	 * @return the created object, if the constructor was successfully executed, without exception
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws CodeExecutionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                                because of an exception in the user code in blockchain, that is allowed to be thrown by the constructor
	 * @throws TransactionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                              because of an exception outside the user code in blockchain, or not allowed to be thrown by the constructor
	 */
	StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException;

	/**
	 * Expands this node's store with a transaction that runs an instance method of an object already in this node's store.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws CodeExecutionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                                because of an exception in the user code in blockchain, that is allowed to be thrown by the method
	 * @throws TransactionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                              because of an exception outside the user code in blockchain, or not allowed to be thrown by the method
	 */
	StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException;

	/**
	 * Expands this node's store with a transaction that runs a static method of a class in this node.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws CodeExecutionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                                because of an exception in the user code in blockchain, that is allowed to be thrown by the method
	 * @throws TransactionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                              because of an exception outside the user code in blockchain, or not allowed to be thrown by the method
	 */
	StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException;

	/**
	 * Runs an instance {@code @@View} method of an object already in this node's store.
	 * The node's store is not expanded, since the execution of the method has no side-effects.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception
	 * @throws TransactionRejectedException if the transaction could not be executed
	 * @throws CodeExecutionException if the transaction could be executed but led to an exception in the user code in blockchain,
	 *                                that is allowed to be thrown by the method
	 * @throws TransactionException if the transaction could be executed but led to an exception outside the user code in blockchain,
	 *                              or that is not allowed to be thrown by the method
	 */
	StorageValue runViewInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException;

	/**
	 * Runs a static {@code @@View} method of a class in this node.
	 * The node's store is not expanded, since the execution of the method has no side-effects.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception
	 * @throws TransactionRejectedException if the transaction could not be executed
	 * @throws CodeExecutionException if the transaction could be executed but led to an exception in the user code in blockchain,
	 *                                that is allowed to be thrown by the method
	 * @throws TransactionException if the transaction could be executed but led to an exception outside the user code in blockchain,
	 *                              or that is not allowed to be thrown by the method
	 */
	StorageValue runViewStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException;

	/**
	 * Posts a transaction that expands the store of this node with a transaction that installs a jar in it.
	 * 
	 * @param request the transaction request
	 * @return the future holding the reference to the transaction where the jar has been installed
	 * @throws TransactionRejectedException if the transaction could not be posted
	 */
	JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException ;

	/**
	 * Posts a transaction that runs a constructor of a class in this node.
	 * 
	 * @param request the request of the transaction
	 * @return the future holding the result of the computation
	 * @throws TransactionRejectedException if the transaction could not be posted
	 */
	CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException;

	/**
	 * Posts a transaction that runs an instance method of an object already in this node's store.
	 * 
	 * @param request the transaction request
	 * @return the future holding the result of the transaction
	 * @throws TransactionRejectedException if the transaction could not be posted
	 */
	CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException;

	/**
	 * Posts a request that runs a static method of a class in this node.
	 * 
	 * @param request the transaction request
	 * @return the future holding the result of the transaction
	 * @throws TransactionRejectedException if the transaction could not be posted
	 */
	CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException;

	/**
	 * The future of a transaction that executes code in a node.
	 * 
	 * @param <V> the type of the value computed by the transaction
	 */
	interface CodeSupplier<V extends StorageValue> {
	
		/**
	     * Waits if necessary for the transaction to complete, and then retrieves its result.
	     *
	     * @return the computed result of the transaction
	     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	     * @throws CodeExecutionException if the transaction could be executed but led to an exception in the user code in blockchain,
	     *                                that is allowed to be thrown by the constructor
	     * @throws TransactionException if the transaction could not be executed and the store of the node has been expanded with a failed transaction
	     */
	    V get() throws TransactionRejectedException, TransactionException, CodeExecutionException;
	}

	 /**
	 * The future of a transaction that stores a jar in a node.
	 */
	interface JarSupplier {

		/**
	     * Waits if necessary for the transaction to complete, and then retrieves its result.
	     *
	     * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
	     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	     * @throws TransactionException if the transaction could not be executed and the store of the node has been expanded with a failed transaction
	     */
	    TransactionReference get() throws TransactionRejectedException, TransactionException;
	}
}