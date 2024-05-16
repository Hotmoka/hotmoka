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
import java.util.function.Function;

import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.StoreException;

/**
 * 
 */
public interface StoreCache {

	/**
	 * The current gas price in this store. This information could be recovered from the store
	 * itself, but this field is used for caching. The gas price might be missing if the
	 * node is not initialized yet.
	 */
	Optional<BigInteger> getGasPrice();

	/**
	 * The current inflation in this store. This information could be recovered from the store
	 * itself, but this field is used for caching. The inflation might be missing if the
	 * node is not initialized yet.
	 */
	OptionalLong getInflation();

	/**
	 * The current consensus configuration in this store. This information could be recovered from the store
	 * itself, but this field is used for caching. The consensus configuration might be missing if the
	 * store has been checked out to a specific root and consequently this cache has not been recomputed yet.
	 */
	ConsensusConfig<?,?> getConfig();

	Optional<StorageReference> getValidators();

	Optional<StorageReference> getGasStation();

	Optional<StorageReference> getVersions();

	/**
	 * Yields a class loader for the given class path, using a cache to avoid regeneration, if possible.
	 * 
	 * @param classpath the class path that must be used by the class loader
	 * @return the class loader
	 * @throws StoreException if the store is not able to complete the operation correctly
	 */
	EngineClassLoader getClassLoader(TransactionReference classpath, Function<TransactionReference, EngineClassLoader> ifMissing);

	boolean signatureIsValid(TransactionReference reference, Function<TransactionReference, Boolean> ifMissing);

	StoreCache setGasPrice(BigInteger gasPrice);

	StoreCache setInflation(long inflation);

	StoreCache setValidators(StorageReference validators);

	StoreCache setGasStation(StorageReference gasStation);

	StoreCache setVersions(StorageReference versions);

	StoreCache setConfig(ConsensusConfig<?,?> consensus);

	StoreCache invalidateClassLoaders();
}