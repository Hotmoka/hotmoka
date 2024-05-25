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

package io.hotmoka.node.local.internal.store;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.StoreCache;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.Store;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.internal.StoreCacheImpl;

/**
 * Partial implementation of a store of a node. It is a container of request/response pairs.
 * Stores are immutable and consequently thread-safe.
 * 
 * @param <S> the type of this store
 * @param <T> the type of the store transformations that can be started from this store
 */
@Immutable
public abstract class AbstractStoreImpl<S extends AbstractStoreImpl<S,T>, T extends AbstractStoreTransformationImpl<S, T>> extends ExecutionEnvironment implements Store<S, T> {

	private final StoreCache cache;

	private final BigInteger maxGasPerView;

	/**
	 * The current consensus configuration in this store, for the execution of view transactions.
	 * This coincides with {@link #consensus} but for the maximum allowed for the gas, which is
	 * fixed to the value specified in the local configuration of the node having this store:
	 * {@link io.hotmoka.node.local.api.LocalNodeConfig#getMaxGasPerViewTransaction()}.
	 */
	private final ConsensusConfig<?,?> consensusForViews;

	private final Hasher<TransactionRequest<?>> hasher;

	private final static Logger LOGGER = Logger.getLogger(AbstractStoreImpl.class.getName());

	/**
	 * Creates a store.
	 * 
	 * @param executors the executors to use for running transactions
	 * @param consensus the consensus configuration of the node having the store
	 * @param config the local configuration of the node having the store
	 * @param hasher the hasher for computing the transaction reference from the requests
	 */
	protected AbstractStoreImpl(ExecutorService executors, ConsensusConfig<?,?> consensus, LocalNodeConfig<?,?> config, Hasher<TransactionRequest<?>> hasher) {
		super(executors);

		this.hasher = hasher;
		this.cache = new StoreCacheImpl(consensus);
		this.maxGasPerView = config.getMaxGasPerViewTransaction();
		this.consensusForViews = consensus.toBuilder().setMaxGasPerTransaction(maxGasPerView).build();
	}

	/**
	 * Creates a clone of a store.
	 * 
	 * @param toClone the store to clone
	 * @param cache to caches to use in the cloned store
	 */
	protected AbstractStoreImpl(AbstractStoreImpl<S, T> toClone, StoreCache cache) {
		super(toClone.getExecutors());

		this.hasher = toClone.hasher;
		this.maxGasPerView = toClone.maxGasPerView;
		this.cache = cache;
		this.consensusForViews = cache.getConfig().toBuilder().setMaxGasPerTransaction(maxGasPerView).build();
	}

	@Override
	public final T beginTransaction(long now) throws StoreException {
		return beginTransformation(getExecutors(), cache.getConfig(), now);
	}

	@Override
	public final T beginViewTransaction() throws StoreException {
		return beginTransformation(getExecutors(), consensusForViews, getNow());
	}

	@Override
	public final void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException, StoreException {
		var reference = TransactionReferences.of(hasher.hash(request));
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
	 * Yields a clone of this store, but for its caches that are initialized with information extracted from this store.
	 * 
	 * @return the resulting store
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	public final S initCaches() throws StoreException {
		var newCache = cache
			.setConfig(extractConsensus())
			.invalidateClassLoaders()
			.setValidators(extractValidators())
			.setGasStation(extractGasStation())
			.setVersions(extractVersions())
			.setGasPrice(extractGasPrice())
			.setInflation(extractInflation());

		return setCache(newCache);
	}

	/**
	 * Yields a store derived from this by setting the given cache and adding the given extra information.
	 * 
	 * @param cache the cache for the resulting store
	 * @param addedRequests the requests to add; by iterating on them, one gets the requests
	 *                      in order of addition to the transformation
	 * @param addedResponses the responses to add
	 * @param addedHistories the histories to add
	 * @param addedManifest the manifest to add, if any
	 * @return the resulting store
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	protected abstract S addDelta(StoreCache cache,
			LinkedHashMap<TransactionReference, TransactionRequest<?>> addedRequests,
			Map<TransactionReference, TransactionResponse> addedResponses,
			Map<StorageReference, TransactionReference[]> addedHistories,
			Optional<StorageReference> addedManifest) throws StoreException;

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
	 * @param executors the executors used to execute transactions in the transformation
	 * @param consensus the consensus configuration at the beginning of the transformation
	 * @param now the time used as current time for the transactions executed in the transformation
	 * @return the transformation
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	protected abstract T beginTransformation(ExecutorService executors, ConsensusConfig<?,?> consensus, long now) throws StoreException;

	@Override
	protected final StoreCache getCache() {
		return cache;
	}

	@Override
	protected final Hasher<TransactionRequest<?>> getHasher() {
		return hasher;
	}

	@Override
	protected final long getNow() {
		return System.currentTimeMillis();
	}

	@Override
	public void free() throws StoreException {
		// nothing by default, but subclasses may redefine
	}

	@Override
	public byte[] getStateId() throws StoreException {
		// we return the hash of this object, as four bytes; subclasses may redefine
		int hash = hashCode();
		return new byte[] { (byte) ((hash >> 24) % 0xff), (byte) ((hash >> 16) % 0xff), (byte) ((hash >> 8) % 0xff), (byte) (hash % 0xff) };
	}
}