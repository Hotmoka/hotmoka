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

package io.hotmoka.node.local.internal.transactions;

import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.LRUCache;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.Store;
import io.hotmoka.node.local.api.StoreException;

@Immutable
public abstract class AbstractStoreImpl<S extends AbstractStoreImpl<S,T>, T extends AbstractStoreTransactionImpl<S, T>> implements Store<S, T> {

	/**
	 * Cached recent transactions whose requests that have had their signature checked.
	 */
	final LRUCache<TransactionReference, Boolean> checkedSignatures;

	/**
	 * Cached class loaders for each classpath.
	 */
	final LRUCache<TransactionReference, EngineClassLoader> classLoaders;

	/**
	 * The current consensus configuration in this store. This information could be recovered from the store
	 * itself, but this field is used for caching. The consensus configuration might be missing if the
	 * store has been checked out to a specific root and consequently this cache has not been recomputed yet.
	 */
	final ConsensusConfig<?,?> consensus;

	final BigInteger maxGasPerView;

	/**
	 * The current consensus configuration in this store, for the execution of view transactions.
	 * This coincides with {@link #consensus} but for the maximum allowed for the gas, which is
	 * fixed to the value specified in the local configuration of the node having this store:
	 * {@link io.hotmoka.node.local.api.LocalNodeConfig#getMaxGasPerViewTransaction()}.
	 */
	private final ConsensusConfig<?,?> consensusForViews;

	/**
	 * The current gas price in this store. This information could be recovered from the store
	 * itself, but this field is used for caching. The gas price might be missing if the
	 * node is not initialized yet.
	 */
	final Optional<BigInteger> gasPrice;

	/**
	 * The current inflation in this store. This information could be recovered from the store
	 * itself, but this field is used for caching. The inflation might be missing if the
	 * node is not initialized yet.
	 */
	final OptionalLong inflation;

	final Hasher<TransactionRequest<?>> hasher;

	private final ExecutorService executors;

	protected AbstractStoreImpl(ExecutorService executors, ConsensusConfig<?,?> consensus, LocalNodeConfig<?,?> config, Hasher<TransactionRequest<?>> hasher) {
		this.executors = executors;
		this.hasher = hasher;
		this.checkedSignatures = new LRUCache<>(100, 1000);
		this.classLoaders = new LRUCache<>(100, 1000);
		this.consensus = consensus;
		this.maxGasPerView = config.getMaxGasPerViewTransaction();
		this.consensusForViews = consensus.toBuilder().setMaxGasPerTransaction(maxGasPerView).build();
		this.gasPrice = Optional.empty();
		this.inflation = OptionalLong.empty();
	}

	protected AbstractStoreImpl(AbstractStoreImpl<S, T> toClone, LRUCache<TransactionReference, Boolean> checkedSignatures, LRUCache<TransactionReference, EngineClassLoader> classLoaders, ConsensusConfig<?,?> consensus, Optional<BigInteger> gasPrice, OptionalLong inflation) {
		this.executors = toClone.executors;
		this.hasher = toClone.hasher;
		this.checkedSignatures = checkedSignatures; //new LRUCache<>(checkedSignatures);
		this.classLoaders = classLoaders; //new LRUCache<>(classLoaders); // TODO: clone?
		this.maxGasPerView = toClone.maxGasPerView;
		this.consensus = consensus;
		this.consensusForViews = consensus.toBuilder().setMaxGasPerTransaction(maxGasPerView).build();
		this.gasPrice = gasPrice;
		this.inflation = inflation;
	}

	@Override
	public final ConsensusConfig<?,?> getConfig() {
		return consensus;
	}

	@Override
	public final T beginTransaction(long now) throws StoreException {
		return beginTransaction(executors, consensus, now);
	}

	@Override
	public final T beginViewTransaction() throws StoreException {
		return beginTransaction(executors, consensusForViews, System.currentTimeMillis());
	}

	protected abstract T beginTransaction(ExecutorService executors, ConsensusConfig<?,?> consensus, long now) throws StoreException;
}