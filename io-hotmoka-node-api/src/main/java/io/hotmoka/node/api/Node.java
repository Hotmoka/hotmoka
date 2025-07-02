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

package io.hotmoka.node.api;

import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.closeables.api.OnCloseHandlersContainer;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.GameteCreationTransactionRequest;
import io.hotmoka.node.api.requests.InitializationTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;

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
public interface Node extends AutoCloseable, OnCloseHandlersContainer {

	/**
	 * Yields the consensus configuration of this node.
	 * 
	 * @return the consensus configuration of this node
	 * @throws ClosedNodeException if the node is already closed
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	ConsensusConfig<?,?> getConfig() throws ClosedNodeException, TimeoutException, InterruptedException;

	/**
	 * Yields the reference to the jar installed in the store of the node, when the node was initialized,
	 * containing the classes of the Takamaka runtime.
	 * If this node has some form of commit, then this method returns a reference
	 * only if the installation of the manifest has been already committed.
	 * 
	 * @return the reference to the jar containing the classes of the Takamaka runtime
	 * @throws NodeException if the node is not able to perform the operation, for instance, if the
	 *                       node is not initialized yet
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	TransactionReference getTakamakaCode() throws NodeException, TimeoutException, InterruptedException; // TODO: throw UnknownReferenceException if uninitialized

	/**
	 * Yields the manifest installed in the store of the node, when the node was initialized.
	 * The manifest is an object of type {@code io.takamaka.code.system.Manifest} that contains
	 * information about the node, useful for the users of the node.
	 * If this node has some form of commit, then this method returns a reference
	 * only if the installation of the manifest has been already committed.
	 * 
	 * @return the reference to the manifest
	 * @throws NodeException if the node is not able to perform the operation, for instance,
	 *                       if the node has not been initialized yet
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	StorageReference getManifest() throws NodeException, TimeoutException, InterruptedException; // TODO: throw UnknownReferenceException if uninitialized

	/**
	 * Yields node-specific information about the node. This is likely different for each node
	 * of the network, hence this is not consensus information.
	 * 
	 * @return the node-specific information about the node
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	NodeInfo getInfo() throws NodeException, TimeoutException, InterruptedException;

	/**
	 * Yields the class tag of the object with the given storage reference.
	 * If this method succeeds and this node has some form of commit, then the transaction
	 * of the storage reference has been definitely committed in this node.
	 * A node is allowed to keep in store all, some or none of the objects.
	 * Hence, this method might fail to find the class tag although the object previously
	 * existed in store.
	 * 
	 * @param object the storage reference of the object
	 * @return the class tag
	 * @throws UnknownReferenceException if {@code object} cannot be found in this node
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	ClassTag getClassTag(StorageReference object) throws UnknownReferenceException, NodeException, TimeoutException, InterruptedException;

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
	 * @throws UnknownReferenceException if {@code object} cannot be found in this node
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	Stream<Update> getState(StorageReference object) throws UnknownReferenceException, NodeException, TimeoutException, InterruptedException;

	/**
	 * Yields the request that generated the transaction with the given reference.
	 * If this node has some form of commit, then this method can only succeed
	 * when the transaction has been definitely committed in this node.
	 * Nodes are allowed to keep in store all, some or none of the requests
	 * that they received during their lifetime.
	 * 
	 * @param reference the reference of the transaction
	 * @return the request
	 * @throws UnknownReferenceException if the request of {@code reference} cannot be found in this node
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, NodeException, TimeoutException, InterruptedException;

	/**
	 * Yields the response generated for the request for the given transaction.
	 * If this node has some form of commit, then this method can only succeed only
	 * when the transaction has been definitely committed in this node.
	 * Nodes are allowed to keep in store all, some or none of the responses
	 * that they computed during their lifetime.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response
	 * @throws UnknownReferenceException if the response of {@code reference} cannot be found in this node
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException, NodeException, TimeoutException, InterruptedException;

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
	 * @throws TransactionRejectedException if the request failed to be committed
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if the polling delay has expired but the request did not get committed
	 * @throws InterruptedException if the current thread has been interrupted while waiting for the response
	 */
	TransactionResponse getPolledResponse(TransactionReference reference) throws TransactionRejectedException, NodeException, TimeoutException, InterruptedException;

	/**
	 * Expands the store of this node with a transaction that
	 * installs a jar in it. It has no caller and requires no gas. The goal is to install, in the
	 * node, some basic jars that are likely needed as dependencies by future jars.
	 * For instance, the jar containing the basic Takamaka classes.
	 * This installation have special privileges, such as that of installing
	 * packages in {@code io.takamaka.code.lang.*}.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if the polling delay has expired but the request did not get committed
	 * @throws InterruptedException if the current thread has been interrupted while waiting for the response
	 */
	TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException, NodeException, TimeoutException, InterruptedException;

	/**
	 * Expands the store of this node with a transaction that creates a gamete, that is,
	 * an externally owned contract with the given initial amount of coins,
	 * of class {@code io.takamaka.code.lang.Gamete}.
	 * This transaction has no caller and requires no gas.
	 * 
	 * @param request the transaction request
	 * @return the reference to the freshly created gamete
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if the polling delay has expired but the request did not get committed
	 * @throws InterruptedException if the current thread has been interrupted while waiting for the response
	 */
	StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException, NodeException, TimeoutException, InterruptedException;

	/**
	 * Expands the store of this node with a transaction that marks the node as
	 * initialized and installs its manifest. After this transaction, no more initial transactions
	 * can be executed on the node.
	 * 
	 * @param request the transaction request
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if the polling delay has expired but the request did not get committed
	 * @throws InterruptedException if the current thread has been interrupted while waiting for the response
	 */
	void addInitializationTransaction(InitializationTransactionRequest request) throws TransactionRejectedException, NodeException, TimeoutException, InterruptedException;

	/**
	 * Expands the store of this node with a transaction that installs a jar in it.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws TransactionException if the transaction could be executed and the store of the node has been expanded with a failed transaction
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException, NodeException, TimeoutException, InterruptedException;

	/**
	 * Expands the store of this node with a transaction that runs a constructor of a class.
	 * 
	 * @param request the request of the transaction
	 * @return the created object, if the constructor was successfully executed, without exception
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws CodeExecutionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                                because of an exception in the user code, that is allowed to be thrown by the constructor
	 * @throws TransactionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                              because of an exception outside the user code in the node, or not allowed to be thrown by the constructor
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException;

	/**
	 * Expands the store of this node with a transaction that runs an instance method of an object already in this node's store.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. This is empty
	 *         if and only if the method is declared to return {@code void}
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws CodeExecutionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                                because of an exception in the user code, that is allowed to be thrown by the method
	 * @throws TransactionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                              because of an exception outside the user code in the node, or not allowed to be thrown by the method
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	Optional<StorageValue> addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException;

	/**
	 * Expands the store of this node with a transaction that runs a static method of a class in this node.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. This is empty
	 *         if and only if the method is declared to return {@code void}
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws CodeExecutionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                                because of an exception in the user code, that is allowed to be thrown by the method
	 * @throws TransactionException if the transaction could be executed and the node has been expanded with a failed transaction,
	 *                              because of an exception outside the user code, or not allowed to be thrown by the method
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	Optional<StorageValue> addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException;

	/**
	 * Runs an instance {@code @@View} method of an object already in this node's store.
	 * The node's store is not expanded, since the execution of the method has no side-effects.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. This is empty
	 *         if and only if the method is declared to return {@code void}
	 * @throws TransactionRejectedException if the transaction could not be executed
	 * @throws CodeExecutionException if the transaction could be executed but led to an exception in the user code,
	 *                                that is allowed to be thrown by the method
	 * @throws TransactionException if the transaction could be executed but led to an exception outside the user code,
	 *                              or that is not allowed to be thrown by the method
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException;

	/**
	 * Runs a static {@code @@View} method of a class in this node.
	 * The node's store is not expanded, since the execution of the method has no side-effects.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. This is empty
	 *         if and only if the method is declared to return {@code void}
	 * @throws TransactionRejectedException if the transaction could not be executed
	 * @throws CodeExecutionException if the transaction could be executed but led to an exception in the user code,
	 *                                that is allowed to be thrown by the method
	 * @throws TransactionException if the transaction could be executed but led to an exception outside the user code,
	 *                              or that is not allowed to be thrown by the method
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	Optional<StorageValue> runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException;

	/**
	 * Posts a transaction that expands the store of this node with a transaction that installs a jar in it.
	 * 
	 * @param request the transaction request
	 * @return the future holding the reference to the transaction where the jar has been installed
	 * @throws TransactionRejectedException if the transaction could not be posted
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	JarFuture postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException;

	/**
	 * Posts a transaction that runs a constructor of a class in this node.
	 * 
	 * @param request the request of the transaction
	 * @return the future holding the result of the computation
	 * @throws TransactionRejectedException if the transaction could not be posted
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	ConstructorFuture postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException;

	/**
	 * Posts a transaction that runs an instance method of an object already in this node's store.
	 * 
	 * @param request the transaction request
	 * @return the future holding the result of the transaction
	 * @throws TransactionRejectedException if the transaction could not be posted
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	MethodFuture postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException;

	/**
	 * Posts a request that runs a static method of a class in this node.
	 * 
	 * @param request the transaction request
	 * @return the future holding the result of the transaction
	 * @throws TransactionRejectedException if the transaction could not be posted
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	MethodFuture postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, NodeException, InterruptedException, TimeoutException;

	/**
	 * Subscribes the given handler for events with the given creator.
	 * 
	 * @param creator the creator of the events that will be forwarded to the handler; if this is {@code null},
	 *                all events will be forwarded to the handler
	 * @param handler a handler that gets executed when an event occurs;
	 *                for each event, it receives the creator of the event and the event itself
	 * @return the subscription, that can be used later to stop event handling with {@code handler}
	 * @throws NodeException if the node is not able to perform the operation
	 */
	Subscription subscribeToEvents(StorageReference creator, BiConsumer<StorageReference, StorageReference> handler) throws NodeException;

	/**
	 * Closes the node.
	 * 
	 * @throws NodeException if the node is not able to close correctly
	 */
	@Override
	void close() throws NodeException;
}