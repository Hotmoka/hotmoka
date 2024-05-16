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
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
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
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.Store;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.internal.StoreCacheImpl;

@Immutable
public abstract class AbstractStoreImpl<S extends AbstractStoreImpl<S,T>, T extends AbstractStoreTransactionImpl<S, T>> extends ExecutionEnvironment implements Store<S, T> {

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

	private final ExecutorService executors;

	private final static Logger LOGGER = Logger.getLogger(AbstractStoreImpl.class.getName());

	protected AbstractStoreImpl(ExecutorService executors, ConsensusConfig<?,?> consensus, LocalNodeConfig<?,?> config, Hasher<TransactionRequest<?>> hasher) {
		this.executors = executors;
		this.hasher = hasher;
		this.cache = new StoreCacheImpl(consensus);
		this.maxGasPerView = config.getMaxGasPerViewTransaction();
		this.consensusForViews = consensus.toBuilder().setMaxGasPerTransaction(maxGasPerView).build();
	}

	protected AbstractStoreImpl(AbstractStoreImpl<S, T> toClone, StoreCache cache) {
		this.executors = toClone.executors;
		this.hasher = toClone.hasher;
		this.maxGasPerView = toClone.maxGasPerView;
		this.cache = cache;
		this.consensusForViews = cache.getConfig().toBuilder().setMaxGasPerTransaction(maxGasPerView).build();
	}

	@Override
	public final ConsensusConfig<?,?> getConfig() {
		return cache.getConfig();
	}

	@Override
	public final T beginTransaction(long now) throws StoreException {
		return beginTransaction(executors, cache.getConfig(), now);
	}

	@Override
	public final T beginViewTransaction() throws StoreException {
		return beginTransaction(executors, consensusForViews, System.currentTimeMillis());
	}

	@Override
	public final void checkTransaction(TransactionRequest<?> request) throws TransactionRejectedException, StoreException {
		var reference = TransactionReferences.of(hasher.hash(request));

		try {
			LOGGER.info(reference + ": checking start");
			responseBuilderFor(reference, request);
			LOGGER.info(reference + ": checking success");
		}
		catch (TransactionRejectedException e) {
			// we do not write the error message in the store, since a failed check request means that nobody
			// is paying for it and therefore we do not want to expand the store; we just take note of the failure,
			// so that getResponse knows which message to use for the rejected transaction exception
			LOGGER.warning(reference + ": checking failed: " + e.getMessage());
			throw e;
		}
	}

	protected abstract S addDelta(StoreCache cache,
			Map<TransactionReference, TransactionRequest<?>> addedRequests,
			Map<TransactionReference, TransactionResponse> addedResponses,
			Map<StorageReference, TransactionReference[]> addedHistories,
			Optional<StorageReference> addedManifest) throws StoreException;

	protected abstract T beginTransaction(ExecutorService executors, ConsensusConfig<?,?> consensus, long now) throws StoreException;

	protected final StoreCache getCache() {
		return cache;
	}

	@Override
	protected final EngineClassLoader getClassLoader(TransactionReference classpath, Function<TransactionReference, EngineClassLoader> ifMissing) {
		return cache.getClassLoader(classpath, ifMissing);
	}

	@Override
	protected final boolean signatureIsValid(TransactionReference classpath, Function<TransactionReference, Boolean> ifMissing) {
		return cache.signatureIsValid(classpath, ifMissing);
	}

	@Override
	protected final ExecutorService getExecutors() {
		return executors;
	}

	@Override
	protected final Hasher<TransactionRequest<?>> getHasher() {
		return hasher;
	}

	@Override
	protected final OptionalLong getInflation() {
		return cache.getInflation();
	}

	@Override
	protected final Optional<StorageReference> getValidators() {
		return cache.getValidators();
	}

	@Override
	protected final Optional<StorageReference> getGasStation() {
		return cache.getGasStation();
	}

	@Override
	protected final Optional<StorageReference> getVersions() {
		return cache.getVersions();
	}

	@Override
	protected final long getNow() {
		return System.currentTimeMillis();
	}
}