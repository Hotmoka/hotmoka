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

package io.hotmoka.stores;

import java.math.BigInteger;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The store of a node. It keeps information about the state of the objects created
 * by the requests executed by the node. A store is external to the node and, typically, only
 * its hash is held in the node, if consensus is needed. Stores must be thread-safe, since they can
 * be used concurrently for executing more requests.
 */
public interface StoreTransaction<T extends Store<T>> {

	Store<?> getStore();
	/**
	 * Yields the time to use as current time for the requests executed performed inside this transaction.
	 * 
	 * @return the time, in milliseconds from the UNIX epoch time
	 */
	long getNow();

	/**
	 * Yields the response of the transaction having the given reference.
	 * This considers also updates inside this transaction, that have not yet been committed.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response, if any
	 */
	Optional<TransactionResponse> getResponseUncommitted(TransactionReference reference) throws StoreException;

	/**
	 * Yields the history of the given object, that is, the references to the transactions
	 * that can be used to reconstruct the current values of its fields.
	 * This considers also updates inside this transaction, that have not yet been committed.
	 * 
	 * @param object the reference of the object
	 * @return the history. Yields an empty stream if there is no history for {@code object}
	 * @throws StoreException if the store is not able to perform the operation
	 */
	Stream<TransactionReference> getHistoryUncommitted(StorageReference object) throws StoreException;

	/**
	 * Yields the manifest installed when the node is initialized.
	 * This considers also updates inside this transaction, that have not yet been committed.
	 * 
	 * @return the manifest
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	Optional<StorageReference> getManifestUncommitted() throws StoreException;

	Optional<TransactionReference> getTakamakaCodeUncommitted() throws StoreException;

	boolean nodeIsInitializedUncommitted() throws StoreException;

	Optional<StorageReference> getGasStationUncommitted() throws StoreException;

	Optional<StorageReference> getValidatorsUncommitted() throws StoreException;

	Optional<StorageReference> getGameteUncommitted() throws StoreException;

	Optional<StorageReference> getVersionsUncommitted() throws StoreException;

	BigInteger getBalanceUncommitted(StorageReference contract);

	BigInteger getRedBalanceUncommitted(StorageReference contract);

	BigInteger getCurrentSupplyUncommitted(StorageReference validators);

	String getPublicKeyUncommitted(StorageReference account);

	StorageReference getCreatorUncommitted(StorageReference event);

	BigInteger getNonceUncommitted(StorageReference account);

	BigInteger getTotalBalanceUncommitted(StorageReference contract);

	String getClassNameUncommitted(StorageReference reference);

	ClassTag getClassTagUncommitted(StorageReference reference) throws NoSuchElementException;

	Stream<UpdateOfField> getEagerFieldsUncommitted(StorageReference object) throws StoreException;

	Optional<UpdateOfField> getLastUpdateToFieldUncommitted(StorageReference object, FieldSignature field) throws StoreException;

	Optional<UpdateOfField> getLastUpdateToFinalFieldUncommitted(StorageReference object, FieldSignature field);

	/**
	 * Pushes the result of executing a successful Hotmoka request.
	 * This method assumes that the given request was not already present in the store.
	 * This method yields a store where the push is visible. Checkable stores remain
	 * unchanged after a call to this method, while non-checkable stores might be
	 * modified and coincide with the result of the method.
	 * 
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 * @return the store resulting after the push
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	void push(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) throws StoreException;

	/**
	 * Pushes into the store the error message resulting from the unsuccessful execution of a Hotmoka request.
	 * 
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param errorMessage the error message
	 * @return the store resulting after the push
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	void push(TransactionReference reference, TransactionRequest<?> request, String errorMessage) throws StoreException;

	/**
	 * Pushes into the store the result of executing a successful Hotmoka request.
	 * This method assumes that the given request was already present in the store.
	 * 
	 * @param reference the reference of the request
	 * @param request the request of the transaction
	 * @param response the response of the transaction
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	void replace(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) throws StoreException;

	T commit() throws StoreException;

	void abort() throws StoreException;
}