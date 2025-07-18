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

package io.hotmoka.node.local.internal;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.api.FieldNotFoundException;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.Store;
import io.hotmoka.node.local.api.StoreCache;
import io.hotmoka.node.local.internal.builders.ExecutionEnvironment;

/**
 * Partial implementation of a store of a node. It is a container of request/response pairs.
 * Stores are immutable and consequently thread-safe.
 * 
 * @param <N> the type of the node having this store
 * @param <C> the type of the configuration of the node having this store
 * @param <S> the type of this store
 * @param <T> the type of the store transformations that can be started from this store
 */
@Immutable
public abstract class AbstractStoreImpl<N extends AbstractLocalNodeImpl<N,C,S,T>, C extends LocalNodeConfig<C,?>, S extends AbstractStoreImpl<N,C,S,T>, T extends AbstractStoreTransformationImpl<N,C,S,T>> extends ExecutionEnvironment implements Store<S,T> {

	/**
	 * The node having this store.
	 */
	private final N node;

	/**
	 * The cache of this store.
	 */
	private final StoreCache cache;

	/**
	 * The current consensus configuration in this store, for the execution of view transactions.
	 * This coincides with the information kept inside the store but for the maximum allowed for the gas,
	 * which is fixed to the value specified in the local configuration of the node having this store:
	 * {@link io.hotmoka.node.local.api.LocalNodeConfig#getMaxGasPerViewTransaction()}.
	 */
	private final ConsensusConfig<?,?> consensusForViews;

	/**
	 * Creates an empty store, with the given cache.
	 * 
	 * @param node the node for which the store is created
	 * @param cache the cache to use in the cloned store; if missing, an empty cache is used
	 */
	private AbstractStoreImpl(N node, Optional<StoreCache> cache) {
		this.node = node;
		this.cache = cache.isPresent() ? cache.get() : new StoreCacheImpl();
		this.consensusForViews = this.cache.getConfig().toBuilder().setMaxGasPerTransaction(node.getLocalConfig().getMaxGasPerViewTransaction()).build();
	}

	/**
	 * Creates an empty store, with the given cache.
	 * 
	 * @param node the node for which the store is created
	 * @param cache the cache to use in the cloned store; if missing, an empty cache is used
	 */
	private AbstractStoreImpl(N node, StoreCache cache) {
		this.node = node;
		this.cache = cache;
		this.consensusForViews = this.cache.getConfig().toBuilder().setMaxGasPerTransaction(node.getLocalConfig().getMaxGasPerViewTransaction()).build();
	}

	/**
	 * Creates an empty store, with empty cache.
	 * 
	 * @param node the node for which the store is created
	 */
	protected AbstractStoreImpl(N node) {
		this(node, Optional.empty());
	}

	/**
	 * Creates a clone of a store, up to the cache.
	 * 
	 * @param toClone the store to clone
	 * @param cache the cache to use in the cloned store
	 */
	protected AbstractStoreImpl(AbstractStoreImpl<N,C,S,T> toClone, StoreCache cache) {
		this(toClone.getNode(), cache);
	}

	/**
	 * Creates a clone of a store, up to the cache.
	 * 
	 * @param toClone the store to clone
	 * @param cache the cache to use in the cloned store; if missing, an empty cache is used
	 */
	protected AbstractStoreImpl(AbstractStoreImpl<N,C,S,T> toClone, Optional<StoreCache> cache) {
		this(toClone.getNode(), cache);
	}

	@Override
	public final T beginTransformation(long now) {
		return beginTransformation(cache.getConfig(), now);
	}

	@Override
	public final T beginViewTransformation() {
		return beginTransformation(consensusForViews, getNow());
	}

	@Override
	public final long getNow() {
		// transactions executed in a store are only view transactions and use the current time of the local machine
		return System.currentTimeMillis();
	}

	@Override
	protected final <X> Future<X> submit(Callable<X> task) {
		return node.getExecutors().submit(task);
	}

	@Override
	protected final StoreCache getCache() {
		return cache;
	}

	@Override
	protected final Hasher<TransactionRequest<?>> getHasher() {
		return node.getHasher();
	}

	/**
	 * Yields a store identical to this, but for its cache, that is set as given.
	 * 
	 * @param cache the cache to set in the resulting store
	 * @return the resulting store
	 */
	protected abstract S withCache(StoreCache cache);

	/**
	 * Yields the node having this store.
	 * 
	 * @return the node having this store
	 */
	protected final N getNode() {
		return node;
	}

	/**
	 * Yields the creator of the given event.
	 * 
	 * @param event the reference to the event
	 * @return the reference to the creator of {@code event}
	 * @throws UnknownReferenceException if {@code event} is not in this store
	 * @throws FieldNotFoundException if the object {@code event} has no {@code creator} field, which means that it is not really an event
	 */
	protected final StorageReference getCreator(StorageReference event) throws UnknownReferenceException, FieldNotFoundException {
		return getReferenceField(event, FieldSignatures.EVENT_CREATOR_FIELD);
	}

	/**
	 * Begins a new store transformation.
	 * 
	 * @param consensus the consensus configuration at the beginning of the transformation
	 * @param now the time used as current time for the transactions executed in the transformation
	 * @return the transformation
	 */
	protected abstract T beginTransformation(ConsensusConfig<?,?> consensus, long now);
}