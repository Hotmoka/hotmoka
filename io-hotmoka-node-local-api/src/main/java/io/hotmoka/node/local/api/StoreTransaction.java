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
import java.util.function.BiConsumer;

import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
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
	S getInitialStore();

	/**
	 * Yields the time to use as current time for the requests executed inside this transaction.
	 * 
	 * @return the time, in milliseconds from the UNIX epoch time
	 */
	long getNow();

	/**
	 * Yields the current consensus configuration of the node.
	 * 
	 * @return the current consensus configuration of the node
	 */
	ConsensusConfig<?,?> getConfig() throws StoreException;

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

	void notifyAllEvents(BiConsumer<StorageReference, StorageReference> notifier) throws StoreException;

	S getFinalStore() throws StoreException;
}