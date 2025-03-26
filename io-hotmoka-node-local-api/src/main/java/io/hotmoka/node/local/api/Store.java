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

package io.hotmoka.node.local.api;

import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The store of a node. It is a container of request/response pairs.
 * Stores are immutable and consequently thread-safe.
 * 
 * @param <S> the type of this store
 * @param <T> the type of the store transformations that can be started from this store
 */
@Immutable
public interface Store<S extends Store<S, T>, T extends StoreTransformation<S, T>> {

	/**
	 * Yields the request contained in this store, with the given reference.
	 * 
	 * @param reference the reference of the transaction generated by the request
	 * @return the request
	 * @throws UnknownReferenceException if no request with the given reference is present in this store
	 * @throws StoreException if this store is unable to complete the operation correctly
	 */
	TransactionRequest<?> getRequest(TransactionReference reference) throws UnknownReferenceException, StoreException;

	/**
	 * Yields the response contained in this store, for a request with the given reference.
	 * 
	 * @param reference the reference of the transaction generated by the request
	 * @return the response
	 * @throws UnknownReferenceException if no response for a request with the given reference is present in this store
	 * @throws StoreException if this store is unable to complete the operation correctly
	 */
	TransactionResponse getResponse(TransactionReference reference) throws UnknownReferenceException, StoreException;

	/**
	 * Yields the history of the given object, that is, the references to the transactions
	 * that have been executed in this store and can be used to reconstruct the current value
	 * of the fields of the object.
	 * 
	 * @param object the reference of the object
	 * @return the history, in order from oldest to newest transactions
	 * @throws UnknownReferenceException if no object with the given reference is present in this store
	 * @throws StoreException if this store is not able to perform the operation
	 */
	Stream<TransactionReference> getHistory(StorageReference object) throws UnknownReferenceException, StoreException;

	/**
	 * Yields the manifest installed in this store.
	 * 
	 * @return the manifest, if any; this might be missing if the store has not executed
	 *         an initialization request yet
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	Optional<StorageReference> getManifest() throws StoreException;

	/**
	 * Yields the consensus to use for the execution of transformations from this store.
	 * 
	 * @return the consensus
	 */
	ConsensusConfig<?,?> getConfig();

	/**
	 * Checks that the given transaction request is valid.
	 * 
	 * @param request the request
	 * @throws TransactionRejectedException if the request is not valid
	 * @throws StoreException if this store is not able to perform the operation
	 */
	void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException, StoreException;

	/**
	 * Starts a store transformation from this store.
	 * 
	 * @param now the time used as current time for the execution of the requests delivered to the transformation
	 * @return the transformation
	 * @throws StoreException if this store is not able to perform the operation
	 */
	T beginTransformation(long now) throws StoreException;

	/**
	 * Starts a view store transformation. It assumes the time of the transformation to be the
	 * current time UTC and the maximal gas allowed for the transaction to be
	 * that given in the local configuration of the node having this store:
	 * {@link LocalNodeConfig#getMaxGasPerViewTransaction()}.
	 * 
	 * @return the transformation
	 * @throws StoreException if this store is not able to perform the operation
	 */
	T beginViewTransformation() throws StoreException;
}