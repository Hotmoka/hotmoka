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

import java.util.concurrent.TimeoutException;

import io.hotmoka.node.api.transactions.TransactionReference;

/**
 * The future of a transaction that executes code in a node.
 * 
 * @param <V> the type of the value computed by the transaction
 */
public interface CodeFuture<V> {

	/**
	 * Yields the reference of the request of the transaction.
	 * 
	 * @return the reference
	 */
	TransactionReference getReferenceOfRequest();

	/**
     * Waits if necessary for the transaction to complete, and then retrieves its result.
     *
     * @return the computed result of the transaction; this is empty only for calls to void methods
     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
     * @throws CodeExecutionException if the transaction could be executed but led to an exception in the user code in blockchain,
     *                                that is allowed to be thrown by the constructor
     * @throws TransactionException if the transaction could be executed and the store of the node has been expanded with a failed transaction
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
     */
	V get() throws TransactionRejectedException, TransactionException, CodeExecutionException, NodeException, TimeoutException, InterruptedException;
}