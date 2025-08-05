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

/**
 * The configuration of a node.
 * 
 * @param <C> the concrete type of the configuration
 * @param <B> the concrete type of the builder
 */
@Immutable
public interface LocalNodeConfig<C extends LocalNodeConfig<C,B>, B extends LocalNodeConfigBuilder<C,B>> {

	/**
	 * Yields the directory where the node's data will be persisted.
	 * It defaults to {@code chain} in the current directory.
	 * 
	 * @return the directory where the node's data will be persisted
	 */
	Path getDir();

	/**
	 * Yields the maximal number of polling attempts, while waiting
	 * for the result of a posted transaction.
	 * 
	 * @return the maximal number of polling attempts
	 */
	long getMaxPollingAttempts();

	/**
	 * Yields the delay of two subsequent polling attempts, in milliseconds,
	 * while waiting for the result of a posted transaction.
	 * This delay is then increased by 10% at each subsequent attempt.
	 * 
	 * @return the delay of two subsequent polling attempts, in milliseconds
	 */
	long getPollingDelay();

	/**
	 * Yields the maximal amount of gas that a view transaction can consume.
	 * 
	 * @return the maximal amount of gas that a view transaction can consume
	 */
	BigInteger getMaxGasPerViewTransaction();

	/**
	 * Yields the maximal size of the index kept for each object.
	 * That is, the index, for each object, will keep at most this number of
	 * transactions, that have affected the object, as history of the object.
	 * 
	 * @return the maximal size of the index kept for each object; use 0 to disable the index
	 */
	int getIndexSize();

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
	B toBuilder();

	@Override
	boolean equals(Object other);

	@Override
	String toString();
}