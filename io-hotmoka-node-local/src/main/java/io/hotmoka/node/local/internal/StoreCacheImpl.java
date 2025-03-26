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

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;

import io.hotmoka.node.ValidatorsConsensusConfigBuilders;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.LRUCache;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.StoreCache;
import io.hotmoka.node.local.api.StoreException;

/**
 * Implementation of a cache of a store or store transformation.
 */
public class StoreCacheImpl implements StoreCache {

	/**
	 * The current gas price in this store. This information could be recovered from the store
	 * itself, but this field is used for caching. The gas price might be missing if the
	 * node is not initialized yet.
	 */
	private final Optional<BigInteger> gasPrice;

	/**
	 * The current inflation in this store. This information could be recovered from the store
	 * itself, but this field is used for caching. The inflation might be missing if the
	 * node is not initialized yet.
	 */
	private final OptionalLong inflation;

	/**
	 * The current consensus configuration in this store. This information could be recovered from the store
	 * itself, but this field is used for caching. The consensus configuration might be missing if the
	 * store has been checked out to a specific root and consequently this cache has not been recomputed yet.
	 */
	private final ConsensusConfig<?,?> consensus;	

	private final Optional<StorageReference> validators;

	private final Optional<StorageReference> gasStation;

	private final Optional<StorageReference> versions;

	/**
	 * Cached class loaders for each classpath.
	 */
	private final LRUCache<TransactionReference, EngineClassLoader> classLoaders;

	/**
	 * Cached recent transactions whose requests have had their signature checked.
	 */
	private final LRUCache<TransactionReference, Boolean> checkedSignatures;

	/**
	 * Creates empty caches.
	 * 
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	public StoreCacheImpl() throws StoreException {
		try {
			this.consensus = ValidatorsConsensusConfigBuilders.defaults().build();
		}
		catch (NoSuchAlgorithmException e) {
			throw new StoreException(e);
		}

		this.gasPrice = Optional.empty();
		this.inflation = OptionalLong.empty();
		this.validators = Optional.empty();
		this.gasStation = Optional.empty();
		this.versions = Optional.empty();
		this.classLoaders = new LRUCache<>(100, 1000);
		this.checkedSignatures = new LRUCache<>(100, 1000);
	}

	private StoreCacheImpl(Optional<BigInteger> gasPrice, OptionalLong inflation, ConsensusConfig<?,?> consensus, Optional<StorageReference> validators, Optional<StorageReference> gasStation, Optional<StorageReference> versions, LRUCache<TransactionReference, EngineClassLoader> classLoaders, LRUCache<TransactionReference, Boolean> checkedSignatures) {
		this.consensus = consensus;
		this.gasPrice = gasPrice;
		this.inflation = inflation;
		this.validators = validators;
		this.gasStation = gasStation;
		this.versions = versions;
		this.classLoaders = classLoaders;
		this.checkedSignatures = checkedSignatures;
	}

	@Override
	public StoreCache setGasPrice(BigInteger gasPrice) {
		return new StoreCacheImpl(Optional.of(gasPrice), inflation, consensus, validators, gasStation, versions, classLoaders, checkedSignatures);
	}

	@Override
	public StoreCache setInflation(long inflation) {
		return new StoreCacheImpl(gasPrice, OptionalLong.of(inflation), consensus, validators, gasStation, versions, classLoaders, checkedSignatures);
	}

	@Override
	public StoreCache setValidators(StorageReference validators) {
		return new StoreCacheImpl(gasPrice, inflation, consensus, Optional.of(validators), gasStation, versions, classLoaders, checkedSignatures);
	}

	@Override
	public StoreCache setGasStation(StorageReference gasStation) {
		return new StoreCacheImpl(gasPrice, inflation, consensus, validators, Optional.of(gasStation), versions, classLoaders, checkedSignatures);
	}

	@Override
	public StoreCache setVersions(StorageReference versions) {
		return new StoreCacheImpl(gasPrice, inflation, consensus, validators, gasStation, Optional.of(versions), classLoaders, checkedSignatures);
	}

	@Override
	public StoreCache setConfig(ConsensusConfig<?,?> consensus) {
		return new StoreCacheImpl(gasPrice, inflation, consensus, validators, gasStation, versions, classLoaders, checkedSignatures);
	}

	@Override
	public StoreCache invalidateClassLoaders() {
		return new StoreCacheImpl(gasPrice, inflation, consensus, validators, gasStation, versions, new LRUCache<>(100, 1000), checkedSignatures);
	}

	@Override
	public Optional<BigInteger> getGasPrice() {
		return gasPrice;
	}

	@Override
	public OptionalLong getInflation() {
		return inflation;
	}

	@Override
	public ConsensusConfig<?, ?> getConfig() {
		return consensus;
	}

	@Override
	public Optional<StorageReference> getValidators() {
		return validators;
	}

	@Override
	public Optional<StorageReference> getGasStation() {
		return gasStation;
	}

	@Override
	public Optional<StorageReference> getVersions() {
		return versions;
	}

	@Override
	public final EngineClassLoader getClassLoader(TransactionReference classpath, Function<TransactionReference, EngineClassLoader> ifMissing) {
		return classLoaders.computeIfAbsent(classpath, ifMissing);
	}

	@Override
	public final boolean signatureIsValid(TransactionReference reference, Function<TransactionReference, Boolean> ifMissing) {
		return checkedSignatures.computeIfAbsent(reference, ifMissing);
	}
}