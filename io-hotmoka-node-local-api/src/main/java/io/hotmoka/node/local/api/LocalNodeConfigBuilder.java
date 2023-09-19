/*
Copyright 2023 Fausto Spoto

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
import java.nio.file.Path;

import io.hotmoka.beans.references.TransactionReference;

/**
 * The builder of a configuration object.
 * 
 * @param <T> the concrete type of the builder
 */
public interface LocalNodeConfigBuilder<T extends LocalNodeConfigBuilder<T>> {

	/**
	 * Sets the maximal amount of gas that a view transaction can consume.
	 * It defaults to 100_000_000.
	 */
	T setMaxGasPerViewTransaction(BigInteger maxGasPerViewTransaction);

	/**
	 * Sets the directory where the node's data will be persisted.
	 * It defaults to {@code chain} in the current directory.
	 * 
	 * @param dir the directory
	 * @return this builder
	 */
	T setDir(Path dir);

	/**
	 * Sets the maximal number of polling attempts, while waiting
	 * for the result of a posted transaction. It defaults to 60.
	 * 
	 * @param maxPollingAttempts the maximal number of polling attempts
	 * @return this builder
	 */
	T setMaxPollingAttempts(long maxPollingAttempts);

	/**
	 * Sets the delay of two subsequent polling attempts, in milliseconds,
	 * while waiting for the result of a posted transaction.
	 * This delay is then increased by 10% at each subsequent attempt.
	 * It defaults to 10.
	 * 
	 * @param pollingDelay the delay
	 * @return this builder
	 */
	T setPollingDelay(long pollingDelay);

	/**
	 * Sets size of the cache for the {@link io.hotmoka.node.api.Node#getRequest(TransactionReference)} method.
	 * It defaults to 1,000.
	 * 
	 * @param requestCacheSize the cache size
	 * @return this builder
	 */
	T setRequestCacheSize(int requestCacheSize);

	/**
	 * Sets size of the cache for the {@link io.hotmoka.node.api.Node#getResponse(TransactionReference)} method.
	 * It defaults to 1,000.
	 * 
	 * @param responseCacheSize the cache size
	 * @return this builder
	 */
	T setResponseCacheSize(int responseCacheSize);

	/**
	 * Builds the configuration.
	 * 
	 * @return the configuration
	 */
	LocalNodeConfig build(); // TODO: should the return type be a generic parameter?
}