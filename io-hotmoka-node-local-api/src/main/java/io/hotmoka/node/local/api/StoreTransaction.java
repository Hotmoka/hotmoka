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

import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.UpdateOfField;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;

/**
 * The store of a node. It keeps information about the state of the objects created
 * by the requests executed by the node. A store is external to the node and, typically, only
 * its hash is held in the node, if consensus is needed. Stores must be thread-safe, since they can
 * be used concurrently for executing more requests.
 */
public interface StoreTransaction<S extends Store<S,T>, T extends StoreTransaction<S,T>> {

	/**
	 * Yields the store from which this transaction begun.
	 * 
	 * @return the store from which this transaction begun
	 */
	S getStore();

	/**
	 * Yields the time to use as current time for the requests executed inside this transaction.
	 * 
	 * @return the time, in milliseconds from the UNIX epoch time
	 */
	long getNow();

	/**
	 * Yields the current gas price at the end of this transaction.
	 * This might be missing if the node is not initialized yet.
	 * 
	 * @return the current gas price at the end of this transaction
	 */
	Optional<BigInteger> getGasPriceUncommitted() throws StoreException;

	/**
	 * Yields the current inflation of the node.
	 * 
	 * @return the current inflation of the node, if the node is already initialized
	 */
	OptionalLong getInflationUncommitted() throws StoreException;

	/**
	 * Yields the current consensus configuration of the node.
	 * 
	 * @return the current consensus configuration of the node
	 */
	ConsensusConfig<?,?> getConfigUncommitted() throws StoreException;

	<X> Future<X> submit(Callable<X> task);

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
	Stream<TransactionReference> getHistoryUncommitted(StorageReference object) throws UnknownReferenceException, StoreException;

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

	Optional<StorageReference> getValidatorsUncommitted() throws StoreException;

	Optional<StorageReference> getGameteUncommitted() throws StoreException;

	String getPublicKeyUncommitted(StorageReference account);

	StorageReference getCreatorUncommitted(StorageReference event);

	BigInteger getNonceUncommitted(StorageReference account) throws UnknownReferenceException, FieldNotFoundException, StoreException;

	BigInteger getTotalBalanceUncommitted(StorageReference contract) throws UnknownReferenceException, FieldNotFoundException, StoreException;

	String getClassNameUncommitted(StorageReference reference) throws UnknownReferenceException, StoreException;

	ClassTag getClassTagUncommitted(StorageReference reference) throws UnknownReferenceException, StoreException;

	Stream<UpdateOfField> getEagerFieldsUncommitted(StorageReference object) throws UnknownReferenceException, StoreException;

	UpdateOfField getLastUpdateToFieldUncommitted(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException;

	UpdateOfField getLastUpdateToFinalFieldUncommitted(StorageReference object, FieldSignature field) throws UnknownReferenceException, FieldNotFoundException, StoreException;

	Optional<StorageValue> runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, TransactionReference reference) throws TransactionRejectedException, TransactionException, CodeExecutionException;

	Optional<StorageValue> runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, TransactionReference reference) throws TransactionRejectedException, TransactionException, CodeExecutionException;

	/**
	 * Rewards the validators with the cost of the gas consumed for the execution of the
	 * requests in this store transaction.
	 * 
	 * @param behaving the space-separated sequence of identifiers of the
	 *                 validators that behaved correctly and will be rewarded
	 * @param misbehaving the space-separated sequence of the identifiers of the validators that
	 *                    misbehaved and must be punished
	 */
	void rewardValidators(String behaving, String misbehaving) throws StoreException;

	void invalidateConsensusCache() throws StoreException;

	/**
	 * Yields the builder of a response for a request of a transaction.
	 * This method can be redefined in subclasses in order to accomodate
	 * new kinds of transactions, specific to a node.
	 * 
	 * @param reference the reference to the transaction that is building the response
	 * @param request the request
	 * @return the builder
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	ResponseBuilder<?,?> responseBuilderFor(TransactionReference reference, TransactionRequest<?> request) throws TransactionRejectedException;

	/**
	 * Builds a response for the given request and adds it to the store of the node.
	 * 
	 * @param request the request
	 * @return the response; if this node has a notion of commit, this response is typically still uncommitted
	 * @throws TransactionRejectedException if the response cannot be built
	 */
	TransactionResponse deliverTransaction(TransactionRequest<?> request) throws TransactionRejectedException, StoreException;

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

	void notifyAllEvents(BiConsumer<StorageReference, StorageReference> notifier);

	boolean signatureIsValidUncommitted(SignedTransactionRequest<?> request, SignatureAlgorithm signatureAlgorithm) throws StoreException;

	/**
	 * Yields a class loader for the given class path, using a cache to avoid regeneration, if possible.
	 * 
	 * @param classpath the class path that must be used by the class loader
	 * @return the class loader
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	EngineClassLoader getClassLoader(TransactionReference classpath, ConsensusConfig<?,?> consensus) throws StoreException;

	S commit() throws StoreException;

	void abort() throws StoreException;
}