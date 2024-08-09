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
import java.util.function.Function;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The caches of a store or store transformation.
 */
public interface StoreCache {

	/**
	 * Yields the current gas price. This information could be recovered from the store
	 * itself, but this field is used for caching. The gas price might be missing if the
	 * node is not initialized yet.
	 * 
	 * @return the current gas price, if any; this might be missing if the node having the store
	 *         is not initialized yet
	 */
	Optional<BigInteger> getGasPrice();

	/**
	 * Yields the current inflation. This information could be recovered from the store
	 * itself, but this field is used for caching. The inflation might be missing if the
	 * node is not initialized yet.
	 * 
	 * @return the current inflation, if any; this might be missing if the node having the store
	 *         is not initialized yet
	 */
	OptionalLong getInflation();

	/**
	 * Yields the current consensus configuration. This information could be recovered from the store
	 * itself, but this field is used for caching. The consensus configuration might be missing if the
	 * store has been checked out to a specific root and consequently this cache has not been recomputed yet.
	 * 
	 * @return the current consensus configuration
	 */
	ConsensusConfig<?,?> getConfig();

	/**
	 * Yields the validators object in store. This information could be recovered from the store
	 * itself, but this field is used for caching. The consensus configuration might be missing if the
	 * store has been checked out to a specific root and consequently this cache has not been recomputed yet.
	 * 
	 * @return the validators object in store, if any; this might be missing if the node having the store
	 *         is not initialized yet
	 */
	Optional<StorageReference> getValidators();

	/**
	 * Yields the gas station object in store. This information could be recovered from the store
	 * itself, but this field is used for caching. The consensus configuration might be missing if the
	 * store has been checked out to a specific root and consequently this cache has not been recomputed yet.
	 * 
	 * @return the gas station object in store, if any; this might be missing if the node having the store
	 *         is not initialized yet
	 */
	Optional<StorageReference> getGasStation();

	/**
	 * Yields the versions object in store. This information could be recovered from the store
	 * itself, but this field is used for caching. The consensus configuration might be missing if the
	 * store has been checked out to a specific root and consequently this cache has not been recomputed yet.
	 * 
	 * @return the versions object in store, if any; this might be missing if the node having the store
	 *         is not initialized yet
	 */
	Optional<StorageReference> getVersions();

	/**
	 * Yields a class loader for the given class path, using a cache to avoid regeneration, if possible.
	 * 
	 * @param classpath the class path that must be used by the class loader
	 * @param ifMissing a computation executed for cache misses
	 * @return the class loader
	 */
	EngineClassLoader getClassLoader(TransactionReference classpath, Function<TransactionReference, EngineClassLoader> ifMissing);

	/**
	 * Yields the result of checking the signature of requests, using a cache to avoid repeated checks, if possible.
	 * 
	 * @param reference the reference of the request
	 * @param ifMissing a computation executed for cache misses
	 * @return true if the signature of the request must successfully checked
	 */
	boolean getValidSignatureOutcome(TransactionReference reference, Function<TransactionReference, Boolean> ifMissing);

	/**
	 * Yields a new cache with a new gas price.
	 * 
	 * @param gasPrice the new gas price
	 * @return the resulting cache
	 */
	StoreCache setGasPrice(BigInteger gasPrice);

	/**
	 * Yields a new cache with a new inflation.
	 * 
	 * @param inflation the new inflation
	 * @return the resulting cache
	 */
	StoreCache setInflation(long inflation);

	/**
	 * Yields a new cache with a new validators object.
	 * 
	 * @param validators the new validators object
	 * @return the resulting cache
	 */
	StoreCache setValidators(StorageReference validators);

	/**
	 * Yields a new cache with a new gas station object.
	 * 
	 * @param gasStation the new gas station object
	 * @return the resulting cache
	 */
	StoreCache setGasStation(StorageReference gasStation);

	/**
	 * Yields a new cache with a new versions object.
	 * 
	 * @param versions the new versions object
	 * @return the resulting cache
	 */
	StoreCache setVersions(StorageReference versions);

	/**
	 * Yields a new cache with a new consensus configuration.
	 * 
	 * @param consensus the new consensus configuration
	 * @return the resulting cache
	 */	
	StoreCache setConfig(ConsensusConfig<?,?> consensus);

	/**
	 * Yields a new cache with empty class loader cache.
	 * 
	 * @return the resulting cache
	 */		
	StoreCache invalidateClassLoaders();
}