/*
Copyright 2021 Fausto Spoto

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

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;

/**
 * The configuration of a node.
 */
@Immutable
public interface LocalNodeConfig {

	/**
	 * Yields the directory where the node's data will be persisted.
	 * It defaults to {@code chain} in the current directory.
	 */
	Path getDir();

	/**
	 * Yields the maximal number of polling attempts, in milliseconds,
	 * while waiting for the result of a posted transaction.
	 * It defaults to 60.
	 */
	int getMaxPollingAttempts();

	/**
	 * Yields the delay of two subsequent polling attempts, in milliseconds,
	 * while waiting for the result of a posted transaction.
	 * This delay is then increased by 10% at each subsequent attempt.
	 * It defaults to 10.
	 */
	int getPollingDelay();

	/**
	 * Yields the size of the cache for the {@link io.hotmoka.node.api.Node#getRequest(TransactionReference)} method.
	 * It defaults to 1,000.
	 */
	int getRequestCacheSize();

	/**
	 * Yields the size of the cache for the {@link io.hotmoka.node.api.Node#getResponse(TransactionReference)} method.
	 * It defaults to 1,000.
	 */
	int getResponseCacheSize();

	/**
	 * Yields the maximal amount of gas that a view transaction can consume.
	 * It defaults to 100_000_000.
	 */
	BigInteger getMaxGasPerViewTransaction();

	/**
	 * Yields a TOML representation of this configuration.
	 * 
	 * @return the TOML representation, as a string
	 */
	String toToml();

	/**
	 * Yields a builder initialized with the information in this object.
	 * 
	 * @return the builder
	 */
	LocalNodeConfigBuilder<?> toBuilder(); // TODO: should the return type be a generic parameter?

	@Override
	boolean equals(Object other);

	@Override
	String toString();
}