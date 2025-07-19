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

import io.hotmoka.exceptions.functions.FunctionWithExceptions1;
import io.hotmoka.exceptions.functions.FunctionWithExceptions2;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The caches of a store or store transformation.
 */
public interface StoreCache {

	/**
	 * Yields the current gas price. This information could be recovered from the store
	 * itself, but this method is used for caching. The gas price might be missing if the
	 * node is not initialized yet.
	 * 
	 * @return the current gas price, if any; this might be missing if the node having the store
	 *         is not initialized yet
	 */
	Optional<BigInteger> getGasPrice();

	/**
	 * Yields the current consensus configuration. This information could be recovered from the store
	 * itself, but this method is used for caching.
	 * 
	 * @return the current consensus configuration
	 */
	ConsensusConfig<?,?> getConfig();

	/**
	 * Yields the validators object in store. This information could be recovered from the store
	 * itself, but this method is used for caching.
	 * 
	 * @return the validators object in store, if any; this might be missing if the node having the store
	 *         is not initialized yet
	 */
	Optional<StorageReference> getValidators();

	/**
	 * Yields the gas station object in store. This information could be recovered from the store
	 * itself, but this method is used for caching.
	 * 
	 * @return the gas station object in store, if any; this might be missing if the node having the store
	 *         is not initialized yet
	 */
	Optional<StorageReference> getGasStation();

	/**
	 * Yields the versions object in store. This information could be recovered from the store
	 * itself, but this method is used for caching.
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
	 * @throws ClassLoaderCreationException if the class loader cannot be created, for instance because the {@code classpath}
	 *                                      refers to some failed transaction
	 */
	EngineClassLoader getClassLoader(TransactionReference classpath, FunctionWithExceptions1<TransactionReference, EngineClassLoader, ClassLoaderCreationException> ifMissing) throws ClassLoaderCreationException;

	/**
	 * Determines if the signature of a request is valid, using a cache to avoid repeated checks, if possible.
	 * 
	 * @param reference the reference of the request
	 * @param ifMissing a computation executed for cache misses
	 * @return true if the signature of the request was successfully checked
	 * @throws UnknownReferenceException if the caller of the request cannot be found in store
	 * @throws FieldNotFoundException if the caller of the request has no field for its public key; hence it is not really an account
	 */
	boolean signatureIsValid(TransactionReference reference, FunctionWithExceptions2<TransactionReference, Boolean, UnknownReferenceException, FieldNotFoundException> ifMissing) throws UnknownReferenceException, FieldNotFoundException;

	/**
	 * Yields a new cache with a new gas price.
	 * 
	 * @param gasPrice the new gas price
	 * @return the resulting cache
	 */
	StoreCache setGasPrice(BigInteger gasPrice);

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
	 * Yields a new cache with empty class loaders cache.
	 * 
	 * @return the resulting cache
	 */		
	StoreCache invalidateClassLoaders();
}