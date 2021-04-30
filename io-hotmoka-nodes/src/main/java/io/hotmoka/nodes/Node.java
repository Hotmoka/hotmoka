/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.nodes;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.ThreadSafe;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
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
@ThreadSafe
public interface Node extends AutoCloseable {

	/**
	 * Yields the reference, in the store of the node, where the base Takamaka base classes are installed.
	 * If this node has some form of commit, then this method returns a reference
	 * only if the installation of the jar with the Takamaka base classes has been
	 * already committed.
	 * 
	 * @throws NoSuchElementException if the node has not been initialized yet
	 */
	TransactionReference getTakamakaCode() throws NoSuchElementException;

	/**
	 * Yields the manifest installed in the store of the node. The manifest is an object of type
	 * {@code io.takamaka.code.system.Manifest} that contains some information about the node,
	 * useful for the users of the node.
	 * If this node has some form of commit, then this method returns a reference
	 * only if the installation of the manifest has been already committed.
	 * 
	 * @return the reference to the node
	 * @throws NoSuchElementException if no manifest has been set for this node
	 */
	StorageReference getManifest() throws NoSuchElementException;

	/**
	 * Yields the class tag of the object with the given storage reference.
	 * If this method succeeds and this node has some form of commit, then the transaction
	 * of the storage reference has been definitely committed in this node.
	 * A node is allowed to keep in store all, some or none of the objects.
	 * Hence, this method might fail to find the class tag although the object previously
	 * existed in store.
	 * 
	 * @param object the storage reference of the object
	 * @return the class tag, if any
	 * @throws NoSuchElementException if there is no object with that reference or
	 *                                if the class tag could not be found
	 */
	ClassTag getClassTag(StorageReference object) throws NoSuchElementException;

	/**
	 * Yields the current state of the object at the given storage reference.
	 * If this method succeeds and this node has some form of commit, then the transaction
	 * of the storage reference has been definitely committed in this node.
	 * A node is allowed to keep in store all, some or none of the objects.
	 * Hence, this method might fail to find the state of the object although the object previously
	 * existed in store.
	 * 
	 * @param object the storage reference of the object
	 * @return the last updates of all its instance fields; these updates include
	 *         the class tag update for the object
	 * @throws NoSuchElementException if there is no object with that reference
	 */
	Stream<Update> getState(StorageReference object) throws NoSuchElementException;

	/**
	 * Yields the name of the algorithm used to sign requests with this node.
	 * 
	 * @return the name of the algorithm
	 */
	String getNameOfSignatureAlgorithmForRequests();

	/**
	 * Yields the request that generated the transaction with the given reference.
	 * If this node has some form of commit, then this method can only succeed
	 * when the transaction has been definitely committed in this node.
	 * Nodes are allowed to keep in store all, some or none of the requests
	 * that they received during their lifetime.
	 * 
	 * @param reference the reference of the transaction
	 * @return the request
	 * @throws NoSuchElementException if there is no request with that reference
	 */
	TransactionRequest<?> getRequest(TransactionReference reference) throws NoSuchElementException;

	/**
	 * Yields the response generated for the request for the given transaction.
	 * If this node has some form of commit, then this method can only succeed
	 * or yield a {@linkplain TransactionRejectedException} only
	 * when the transaction has been definitely committed in this node.
	 * Nodes are allowed to keep in store all, some or none of the responses
	 * that they computed during their lifetime.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response
	 * @throws TransactionRejectedException if there is a request for that transaction but it failed with this exception
	 * @throws NoSuchElementException if there is no request, and hence no response, with that reference
	 */
	TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException, NoSuchElementException;

	/**
	 * Waits until a transaction has been committed, or until its delivering fails.
	 * If this method succeeds and this node has some form of commit, then the
	 * transaction has been definitely committed.
	 * Nodes are allowed to keep in store all, some or none of the responses
	 * computed during their lifetime. Hence, this method might time out also
	 * when a response has been computed in the past for the transaction of {@code reference},
	 * but it has not been kept in store.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response computed for {@code request}
	 * @throws TransactionRejectedException if the request failed to be committed, because of this exception
	 * @throws TimeoutException if the polling delay has expired but the request did not get committed
	 * @throws InterruptedException if the current thread has been interrupted while waiting for the response
	 */
	TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, TimeoutException, InterruptedException;

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
	 * a red/green externally owned contract with the given initial amount of coins,
	 * of class {@code io.takamaka.code.lang.Gamete}.
	 * This transaction has no caller and requires no gas.
	 * 
	 * @param request the transaction request
	 * @return the reference to the freshly created gamete
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 */
	StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException;

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
	 * @throws TransactionException if the transaction could be executed and the store of the node has been expanded with a failed transaction
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
	StorageValue runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException;

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
	StorageValue runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException;

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
	 * Subscribes the given handler for events with the given creator.
	 * 
	 * @param creator the creator of the events that will be forwarded to the handler; if this is {@code null},
	 *                all events will be forwarded to the handler
	 * @param handler a handler that gets executed when an event with the given creator occurs; a handler can be
	 *                subscribed to more creators; for each event, it receives its creator and the event itself
	 * @return the subscription, that can be used later to stop event handling with {@code handler}
	 */
	Subscription subscribeToEvents(StorageReference creator, BiConsumer<StorageReference, StorageReference> handler);

	/*
	 * A subscription to events generated by a node.
	 */
	interface Subscription extends AutoCloseable {

		/**
		 * Closes the subscription, so that no more events are sent to its handler.
		 */
		@Override
		void close();
	}

	/**
	 * The future of a transaction that executes code in a node.
	 * 
	 * @param <V> the type of the value computed by the transaction
	 */
	interface CodeSupplier<V extends StorageValue> {

		/**
		 * Yields the reference of the request of the transaction.
		 * 
		 * @return the reference
		 */
		TransactionReference getReferenceOfRequest();

		/**
	     * Waits if necessary for the transaction to complete, and then retrieves its result.
	     *
	     * @return the computed result of the transaction
	     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	     * @throws CodeExecutionException if the transaction could be executed but led to an exception in the user code in blockchain,
	     *                                that is allowed to be thrown by the constructor
	     * @throws TransactionException if the transaction could be executed and the store of the node has been expanded with a failed transaction
	     */
	    V get() throws TransactionRejectedException, TransactionException, CodeExecutionException;
	}

	/**
	 * The future of a transaction that stores a jar in a node.
	 */
	interface JarSupplier {

		/**
		 * Yields the reference of the request of the transaction.
		 * 
		 * @return the reference
		 */
		TransactionReference getReferenceOfRequest();

		/**
	     * Waits if necessary for the transaction to complete, and then retrieves its result.
	     *
	     * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
	     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	     * @throws TransactionException if the transaction could be executed and the store of the node has been expanded with a failed transaction
	     */
	    TransactionReference get() throws TransactionRejectedException, TransactionException;
	}
}