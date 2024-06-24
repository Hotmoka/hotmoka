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
	 * @throws CodeExecutionException if the request has been accepted but its execution failed with a user exception in the Takamaka code
	 * @throws StoreException if the final store is not able to complete the operation correctly
	 */
	Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, TransactionReference reference) throws TransactionRejectedException, TransactionException, CodeExecutionException, StoreException;

	/**
	 * Runs the given request of execution of a view static method, at the final store of this transformation.
	 * 
	 * @param request the request to run
	 * @param reference the reference of the request
	 * @return the result of the execution
	 * @throws TransactionRejectedException if the request has been rejected
	 * @throws TransactionException if the request has been accepted but its execution failed with an exception not in the Takamaka code
	 * @throws CodeExecutionException if the request has been accepted but its execution failed with a user exception in the Takamaka code
	 * @throws StoreException if the final store is not able to complete the operation correctly
	 */
	Optional<StorageValue> runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, TransactionReference reference) throws TransactionRejectedException, TransactionException, CodeExecutionException, StoreException;

	/**
	 * Rewards the validators with the cost of the gas consumed for the execution of the
	 * requests delivered in this store transformation.
	 * 
	 * @param behaving the space-separated sequence of identifiers of the
	 *                 validators that behaved correctly and will be rewarded
	 * @param misbehaving the space-separated sequence of the identifiers of the validators that
	 *                    misbehaved and must be punished
	 * @throws StoreException if the final store is not able to complete the operation correctly
	 */
	void deliverRewardTransaction(String behaving, String misbehaving) throws StoreException;

	/**
	 * Builds a response for the given request, executed at the current final store,
	 * and advances the final store to the resulting new store.
	 * 
	 * @param request the request
	 * @return the response
	 * @throws TransactionRejectedException if the request has been rejected
	 * @throws StoreException if the final store of this transformation is not able to complete the operation correctly
	 */
	TransactionResponse deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException, StoreException;

	/**
	 * Yields the number of successfully requests delivered through {@link #deliverTransaction(TransactionRequest)}.
	 * 
	 * @return the number of successfully delivered requests
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	int deliveredCount() throws StoreException;

	/**
	 * Yields the final store of this transformation, resulting from the execution of the delivered requests
	 * from the initial store.
	 * 
	 * @return the final store
	 * @throws StoreException if the final store cannot be computed correctly
	 */
	S getFinalStore() throws StoreException; // TODO: remove at the end
}