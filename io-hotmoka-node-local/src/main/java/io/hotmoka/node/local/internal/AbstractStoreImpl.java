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

import java.util.logging.Logger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.api.FieldNotFoundException;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.Store;
import io.hotmoka.node.local.api.StoreCache;
import io.hotmoka.node.local.api.StoreException;
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
	 * This coincides with {@link #consensus} but for the maximum allowed for the gas, which is
	 * fixed to the value specified in the local configuration of the node having this store:
	 * {@link io.hotmoka.node.local.api.LocalNodeConfig#getMaxGasPerViewTransaction()}.
	 */
	private final ConsensusConfig<?,?> consensusForViews;

	private final static Logger LOGGER = Logger.getLogger(AbstractStoreImpl.class.getName());

	/**
	 * Creates an empty store, with empty cache.
	 * 
	 * @param node the node for which the store is created
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	protected AbstractStoreImpl(N node) throws StoreException {
		super(node.getExecutors());

		this.node = node;
		this.cache = new StoreCacheImpl();

		try {
			this.consensusForViews = cache.getConfig().toBuilder().setMaxGasPerTransaction(node.getLocalConfig().getMaxGasPerViewTransaction()).build();
		}
		catch (NodeException e) {
			throw new StoreException(e);
		}
	}

	/**
	 * Creates a clone of a store, up to the cache.
	 * 
	 * @param toClone the store to clone
	 * @param cache the cache to use in the cloned store
	 */
	protected AbstractStoreImpl(AbstractStoreImpl<N,C,S,T> toClone, StoreCache cache) {
		super(toClone.getNode().getExecutors());

		this.node = toClone.getNode();
		this.cache = cache;
		this.consensusForViews = cache.getConfig().toBuilder().setMaxGasPerTransaction(toClone.consensusForViews.getMaxGasPerTransaction()).build();
	}

	@Override
	public final T beginTransformation(long now) throws StoreException {
		return beginTransformation(cache.getConfig(), now);
	}

	@Override
	public final T beginViewTransformation() throws StoreException {
		return beginTransformation(consensusForViews, getNow());
	}

	@Override
	public final void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException, StoreException {
		var reference = TransactionReferences.of(getHasher().hash(request));
		String referenceAsString = reference.toString();

		try {
			LOGGER.info(referenceAsString + ": checking start");
			responseBuilderFor(reference, request);
			LOGGER.info(referenceAsString + ": checking success");
		}
		catch (TransactionRejectedException e) {
			// we do not write the error message in the store, since a failed check request means that nobody
			// is paying for it and therefore we do not want to expand the store; we just take note of the failure,
			// so that getResponse knows which message to use for the rejected transaction exception
			LOGGER.warning(referenceAsString + ": checking failed: " + e.getMessage());
			throw e;
		}
	}

	/**
	 * Yields a store identical to this, but for its cache, that is set as given.
	 * 
	 * @param cache the cache to set in the resulting store
	 * @return the resulting store
	 */
	protected abstract S setCache(StoreCache cache);

	/**
	 * Begins a new store transformation.
	 * 
	 * @param consensus the consensus configuration at the beginning of the transformation
	 * @param now the time used as current time for the transactions executed in the transformation
	 * @return the transformation
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	protected abstract T beginTransformation(ConsensusConfig<?,?> consensus, long now) throws StoreException;

	/**
	 * Yields the node having this store.
	 * 
	 * @return the node having this store
	 */
	protected final N getNode() {
		return node;
	}

	@Override
	protected final StoreCache getCache() {
		return cache;
	}

	@Override
	protected final Hasher<TransactionRequest<?>> getHasher() {
		return getNode().getHasher();
	}

	protected final StorageReference getCreator(StorageReference event) throws UnknownReferenceException, FieldNotFoundException, StoreException {
		return getReferenceField(event, FieldSignatures.EVENT_CREATOR_FIELD);
	}

	@Override
	public final long getNow() {
		return System.currentTimeMillis();
	}
}