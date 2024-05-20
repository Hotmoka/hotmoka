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

/**
 * The builder of a configuration object.
 * 
 * @param <C> the concrete type of the configuration
 * @param <B> the concrete type of the builder
 */
public interface LocalNodeConfigBuilder<C extends LocalNodeConfig<C,B>, B extends LocalNodeConfigBuilder<C,B>> {

	/**
	 * Sets the maximal amount of gas that a view transaction can consume.
	 * It defaults to 100_000_000.
	 * 
	 * @param maxGasPerViewTransaction the maximal amount of gas that a view transaction can consume
	 * @return this builder
	 */
	B setMaxGasPerViewTransaction(BigInteger maxGasPerViewTransaction);

	/**
	 * Sets the directory where the node's data will be persisted.
	 * It defaults to {@code chain} in the current directory.
	 * 
	 * @param dir the directory
	 * @return this builder
	 */
	B setDir(Path dir);

	/**
	 * Sets the maximal number of polling attempts, while waiting
	 * for the result of a posted transaction. It defaults to 60.
	 * 
	 * @param maxPollingAttempts the maximal number of polling attempts
	 * @return this builder
	 */
	B setMaxPollingAttempts(long maxPollingAttempts);

	/**
	 * Sets the delay of two subsequent polling attempts, in milliseconds,
	 * while waiting for the result of a posted transaction.
	 * This delay is then increased by 10% at each subsequent attempt.
	 * It defaults to 10.
	 * 
	 * @param pollingDelay the delay
	 * @return this builder
	 */
	B setPollingDelay(long pollingDelay);

	/**
	 * Builds the configuration.
	 * 
	 * @return the configuration
	 */
	C build();
}