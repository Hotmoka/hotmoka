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

package io.hotmoka.local.internal;

import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InitialTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.instrumentation.api.GasCostModel;
import io.hotmoka.local.Config;
import io.hotmoka.local.NodeCaches;
import io.hotmoka.local.Store;
import io.hotmoka.local.StoreUtilities;

/**
 * The methods of a Hotmoka node that are used inside the internal
 * implementation of this module. This interface allows to enlarge
 * the visibility of some methods, only for the classes the implement
 * the module.
 */
public interface NodeInternal {

	/**
	 * Yields the configuration of this node.
	 * 
	 * @return the configuration
	 */
	Config getConfig();

	/**
	 * Yields the caches of this node.
	 * 
	 * @return the caches
	 */
	NodeCaches getCaches();

	/**
	 * Yields the gas cost model of this node.
	 * 
	 * @return the gas model
	 */
	GasCostModel getGasCostModel();

	/**
	 * Yields the store of this node.
	 * 
	 * @return the store of this node
	 */
	Store getStore();

	/**
	 * Yields an object that provides methods for reconstructing data from the store of this node.
	 * 
	 * @return the store utilities
	 */
	StoreUtilities getStoreUtilities();

	/**
	 * Yields the base cost of the given transaction. Normally, this is just
	 * {@code request.size()}, but subclasses might redefine.
	 * 
	 * @param request the request of the transaction
	 * @return the base cost of the transaction
	 */
	int getRequestStorageCost(NonInitialTransactionRequest<?> request);

	/**
	 * Determines if the given initial transaction can still be run after the
	 * initialization of the node. Normally, this is false. However, specific
	 * implementations of the node might redefine and allow it.
	 * 
	 * @param request the request
	 * @return true if only if the execution of {@code request} is allowed
	 *         also after the initialization of this node
	 */
	boolean admitsAfterInitialization(InitialTransactionRequest<?> request);

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
	 * Runs the given task with the executor service of this node.
	 * 
	 * @param <T> the type of the result of the task
	 * @param task the task
	 * @return the value computed by the task
	 */
	<T> Future<T> submit(Callable<T> task);

	/**
	 * Runs the given task with the executor service of this node.
	 * 
	 * @param task the task
	 */
	void submit(Runnable task);
}