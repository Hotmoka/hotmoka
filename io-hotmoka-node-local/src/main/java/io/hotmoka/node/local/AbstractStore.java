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

package io.hotmoka.node.local;

import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalLong;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.Store;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.api.StoreTransaction;

@Immutable
public abstract class AbstractStore<S extends AbstractStore<S, N>, N extends AbstractLocalNode<N, ?, S>> implements Store<S, N> {

	/**
	 * Cached recent transactions whose requests that have had their signature checked.
	 */
	final LRUCache<TransactionReference, Boolean> checkedSignatures;

	/**
	 * Cached class loaders for each classpath.
	 */
	final LRUCache<TransactionReference, EngineClassLoader> classLoaders;

	/**
	 * The node for which this store has been created.
	 */
	private final N node;

	/**
	 * The current consensus configuration in this store. This information could be recovered from the store
	 * itself, but this field is used for caching. The consensus configuration might be missing if the
	 * store has been checked out to a specific root and consequently this cache has not been recomputed yet.
	 */
	final ConsensusConfig<?,?> consensus;

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

	protected AbstractStore(N node, ConsensusConfig<?,?> consensus, Hasher<TransactionRequest<?>> hasher) {
		try {
			this.node = node;
			this.hasher = hasher;
			this.checkedSignatures = new LRUCache<>(100, 1000);
			this.classLoaders = new LRUCache<>(100, 1000);
			this.consensus = consensus;
			this.consensusForViews = consensus.toBuilder().setMaxGasPerTransaction(node.getLocalConfig().getMaxGasPerViewTransaction()).build();
			this.gasPrice = Optional.empty();
			this.inflation = OptionalLong.empty();
		}
		catch (NodeException e) {
			throw new RuntimeException(e); // TODO
		}
	}

	protected AbstractStore(AbstractStore<S, N> toClone) {
		this.node = toClone.node;
		this.hasher = toClone.hasher;
		this.checkedSignatures = toClone.checkedSignatures;
		this.classLoaders = toClone.classLoaders;
		this.consensus = toClone.consensus;
		this.consensusForViews = toClone.consensusForViews;
		this.gasPrice = toClone.gasPrice;
		this.inflation = toClone.inflation;
	}

	protected AbstractStore(AbstractStore<S, N> toClone, LRUCache<TransactionReference, Boolean> checkedSignatures, LRUCache<TransactionReference, EngineClassLoader> classLoaders, ConsensusConfig<?,?> consensus, Optional<BigInteger> gasPrice, OptionalLong inflation) {
		try {
			this.node = toClone.node;
			this.hasher = toClone.hasher;
			this.checkedSignatures = checkedSignatures; //new LRUCache<>(checkedSignatures);
			this.classLoaders = classLoaders; //new LRUCache<>(classLoaders); // TODO: clone
			this.consensus = consensus;
			this.consensusForViews = consensus.toBuilder().setMaxGasPerTransaction(node.getLocalConfig().getMaxGasPerViewTransaction()).build();
			this.gasPrice = gasPrice;
			this.inflation = inflation;
		}
		catch (NodeException e) {
			throw new RuntimeException(e); // TODO
		}
	}

	@Override
	public final N getNode() {
		return node;
	}

	public final ConsensusConfig<?,?> getConfig() {
		return consensus;
	}

	@Override
	public final StoreTransaction<S> beginTransaction(long now) throws StoreException {
		return beginTransaction(consensus, now);
	}

	@Override
	public final StoreTransaction<S> beginViewTransaction() throws StoreException {
		return beginTransaction(consensusForViews, System.currentTimeMillis());
	}

	protected abstract StoreTransaction<S> beginTransaction(ConsensusConfig<?,?> consensus, long now) throws StoreException;
}