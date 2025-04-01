/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.local.api;

import java.util.Optional;

import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageValue;

/**
 * A transformation of a store. It is a modification of an initial store towards a final store,
 * induced by the execution of requests.
 * 
 * @param <S> the type of store used in this transformation
 * @param <T> the type of this transformation
 */
public interface StoreTransformation<S extends Store<S,T>, T extends StoreTransformation<S,T>> {

	/**
	 * Yields the store from which this transformation begun.
	 * 
	 * @return the store from which this transformation begun
	 */
	S getInitialStore();

	/**
	 * Runs the given request of execution of a view instance method, at the final store of this transformation.
	 * 
	 * @param request the request to run
	 * @param reference the reference of the request
	 * @return the result of the execution
	 * @throws TransactionRejectedException if the request has been rejected
	 * @throws TransactionException if the request has been accepted but its execution failed with an exception not in the Takamaka code
	 * @throws CodeExecutionException if the request has been accepted but its execution failed with an exception in the Takamaka code
	 * @throws StoreException if the final store is not able to complete the operation correctly
	 * @throws InterruptedException if the current thread is interrupted before computing the result
	 */
	Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, TransactionReference reference) throws TransactionRejectedException, TransactionException, CodeExecutionException, StoreException, InterruptedException;

	/**
	 * Runs the given request of execution of a view static method, at the final store of this transformation.
	 * 
	 * @param request the request to run
	 * @param reference the reference of the request
	 * @return the result of the execution
	 * @throws TransactionRejectedException if the request has been rejected
	 * @throws TransactionException if the request has been accepted but its execution failed with an exception not in the Takamaka code
	 * @throws CodeExecutionException if the request has been accepted but its execution failed with an exception in the Takamaka code
	 * @throws StoreException if the final store is not able to complete the operation correctly
	 * @throws InterruptedException if the current thread is interrupted before computing the result
	 */
	Optional<StorageValue> runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, TransactionReference reference) throws TransactionRejectedException, TransactionException, CodeExecutionException, StoreException, InterruptedException;

	/**
	 * Builds a response for the given request, executed at the current final store,
	 * and advances the final store to the resulting new store.
	 * 
	 * @param request the request
	 * @return the response
	 * @throws TransactionRejectedException if the request has been rejected
	 * @throws StoreException if the final store of this transformation is not able to complete the operation correctly
	 * @throws InterruptedException if the current thread is interrupted before delivering the transaction
	 */
	TransactionResponse deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException, StoreException, InterruptedException;

	/**
	 * Yields the number of successfully requests delivered through {@link #deliverTransaction(TransactionRequest)}.
	 * 
	 * @return the number of successfully delivered requests
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	int deliveredCount() throws StoreException;
}