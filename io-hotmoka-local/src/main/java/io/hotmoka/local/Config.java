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

package io.hotmoka.local;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.hotmoka.beans.references.TransactionReference;

/**
 * The configuration of a node.
 */
public class Config {

	/**
	 * The directory where the node's data will be persisted.
	 * It defaults to {@code chain} in the current directory.
	 */
	public final Path dir;

	/**
	 * The maximal number of polling attempts, in milliseconds,
	 * while waiting for the result of a posted transaction.
	 * It defaults to 60.
	 */
	public final int maxPollingAttempts;

	/**
	 * The delay of two subsequent polling attempts, in milliseconds,
	 * while waiting for the result of a posted transaction.
	 * This delay is then increased by 10% at each subsequent attempt.
	 * It defaults to 10.
	 */
	public final int pollingDelay;

	/**
	 * The size of the cache for the {@link io.hotmoka.nodes.Node#getRequest(TransactionReference)} method.
	 * It defaults to 1,000.
	 */
	public final int requestCacheSize;

	/**
	 * The size of the cache for the {@link io.hotmoka.nodes.Node#getResponse(TransactionReference)} method.
	 * It defaults to 1,000.
	 */
	public final int responseCacheSize;

	/**
	 * The maximal amount of gas that a view transaction can consume.
	 * It defaults to 1_000_000.
	 */
	public final BigInteger maxGasPerViewTransaction;

	/**
	 * Full constructor for the builder pattern.
	 */
	private Config(Path dir, int maxPollingAttempts,
			       int pollingDelay, int requestCacheSize,
			       int responseCacheSize, BigInteger maxGasPerViewTransaction) {

		this.dir = dir;
		this.maxPollingAttempts = maxPollingAttempts;
		this.pollingDelay = pollingDelay;
		this.requestCacheSize = requestCacheSize;
		this.responseCacheSize = responseCacheSize;
		this.maxGasPerViewTransaction = maxGasPerViewTransaction;
	}

	/**
	 * Copy-constructor for subclassing.
	 */
	protected Config(Config parent) {
		this.dir = parent.dir;
		this.maxPollingAttempts = parent.maxPollingAttempts;
		this.pollingDelay = parent.pollingDelay;
		this.requestCacheSize = parent.requestCacheSize;
		this.responseCacheSize = parent.responseCacheSize;
		this.maxGasPerViewTransaction = parent.maxGasPerViewTransaction;
	}

	/**
	 * The builder of a configuration object.
	 */
	public abstract static class Builder<T extends Builder<T>> {
		private Path dir = Paths.get("chain");
		private int maxPollingAttempts = 60;
		private int pollingDelay = 10;
		private int requestCacheSize = 1_000;
		private int responseCacheSize = 1_000;
		private BigInteger maxGasPerViewTransaction = BigInteger.valueOf(1_000_000);

		/**
		 * Standard design pattern. See http://www.angelikalanger.com/GenericsFAQ/FAQSections/ProgrammingIdioms.html#FAQ205
		 */
		protected abstract T getThis();

		/**
		 * Sets the maximal amount of gas that a view transaction can consume.
		 * It defaults to 1_000_000.
		 */
		public T setMaxGasPerViewTransaction(BigInteger maxGasPerViewTransaction) {
			if (maxGasPerViewTransaction == null)
				throw new NullPointerException("the maximal amount of gas per transaction cannot be null");

			this.maxGasPerViewTransaction = maxGasPerViewTransaction;
	
			return getThis();
		}

		/**
		 * Sets the directory where the node's data will be persisted.
		 * It defaults to {@code chain} in the current directory.
		 * 
		 * @param dir the directory
		 * @return this builder
		 */
		public T setDir(Path dir) {
			this.dir = dir;
			return getThis();
		}

		/**
		 * Sets the maximal number of polling attempts, in milliseconds,
		 * while waiting for the result of a posted transaction.
		 * It defaults to 60.
		 * 
		 * @param maxPollingAttempts the the maximal number of polling attempts
		 * @return this builder
		 */
		public T setMaxPollingAttempts(int maxPollingAttempts) {
			this.maxPollingAttempts = maxPollingAttempts;
			return getThis();
		}

		/**
		 * Sets the delay of two subsequent polling attempts, in milliseconds,
		 * while waiting for the result of a posted transaction.
		 * This delay is then increased by 10% at each subsequent attempt.
		 * It defaults to 10.
		 * 
		 * @param pollingDelay the delay
		 * @return this builder
		 */
		public T setPollingDelay(int pollingDelay) {
			this.pollingDelay = pollingDelay;
			return getThis();
		}

		/**
		 * Sets size of the cache for the {@link io.hotmoka.nodes.Node#getRequest(TransactionReference)} method.
		 * It defaults to 1,000.
		 * 
		 * @param requestCacheSize the cache size
		 * @return this builder
		 */
		public T setRequestCacheSize(int requestCacheSize) {
			this.requestCacheSize = requestCacheSize;
			return getThis();
		}

		/**
		 * Sets size of the cache for the {@link io.hotmoka.nodes.Node#getResponse(TransactionReference)} method.
		 * It defaults to 1,000.
		 * 
		 * @param responseCacheSize the cache size
		 * @return this builder
		 */
		public T setResponseCacheSize(int responseCacheSize) {
			this.responseCacheSize = responseCacheSize;
			return getThis();
		}

		/**
		 * Builds the configuration.
		 * 
		 * @return the configuration
		 */
		public Config build() {
			return new Config(dir, maxPollingAttempts, pollingDelay, requestCacheSize, responseCacheSize, maxGasPerViewTransaction);
		}
	}
}