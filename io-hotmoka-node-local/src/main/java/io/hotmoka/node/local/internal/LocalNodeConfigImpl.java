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

package io.hotmoka.node.local.internal;

import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import com.moandjiezana.toml.Toml;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.LocalNodeConfigBuilder;

/**
 * The configuration of a node.
 */
@Immutable
public abstract class LocalNodeConfigImpl implements LocalNodeConfig {

	/**
	 * The directory where the node's data will be persisted.
	 * It defaults to {@code chain} in the current directory.
	 */
	public final Path dir;

	/**
	 * The maximal number of polling attempts, while waiting for the result
	 * of a posted transaction. It defaults to 60.
	 */
	public final long maxPollingAttempts;

	/**
	 * The delay of two subsequent polling attempts, in milliseconds,
	 * while waiting for the result of a posted transaction.
	 * This delay is then increased by 10% at each subsequent attempt.
	 * It defaults to 10.
	 */
	public final long pollingDelay;

	/**
	 * The size of the cache for the {@link io.hotmoka.node.api.Node#getRequest(TransactionReference)} method.
	 * It defaults to 1,000.
	 */
	public final int requestCacheSize;

	/**
	 * The size of the cache for the {@link io.hotmoka.node.api.Node#getResponse(TransactionReference)} method.
	 * It defaults to 1,000.
	 */
	public final int responseCacheSize;

	/**
	 * The maximal amount of gas that a view transaction can consume.
	 * It defaults to 100_000_000.
	 */
	public final BigInteger maxGasPerViewTransaction;

	/**
	 * Creates a new configuration object from its builder.
	 * 
	 * @param the builder
	 */
	protected LocalNodeConfigImpl(ConfigBuilderImpl<?> builder) {
		this.dir = builder.dir;
		this.maxPollingAttempts = builder.maxPollingAttempts;
		this.pollingDelay = builder.pollingDelay;
		this.requestCacheSize = builder.requestCacheSize;
		this.responseCacheSize = builder.responseCacheSize;
		this.maxGasPerViewTransaction = builder.maxGasPerViewTransaction;
	}

	@Override
	public Path getDir() {
		return dir;
	}

	@Override
	public long getMaxPollingAttempts() {
		return maxPollingAttempts;
	}

	@Override
	public long getPollingDelay() {
		return pollingDelay;
	}

	@Override
	public int getRequestCacheSize() {
		return requestCacheSize;
	}

	@Override
	public int getResponseCacheSize() {
		return responseCacheSize;
	}

	@Override
	public BigInteger getMaxGasPerViewTransaction() {
		return maxGasPerViewTransaction;
	}

	@Override
	public String toToml() {
		var sb = new StringBuilder();

		sb.append("# This is a TOML config file for a local Hotmoka node.\n");
		sb.append("# For more information about TOML, see https://github.com/toml-lang/toml\n");
		sb.append("# For more information about Hotmoka, see https://www.hotmoka.io\n");
		sb.append("\n");
		sb.append("# the directory where the node's data will be persisted\n");
		sb.append("dir = \"" + dir + "\"\n");
		sb.append("\n");
		sb.append("# the maximal number of polling attempts, while waiting for the result of a posted transaction\n");
		sb.append("max_polling_attempts = " + maxPollingAttempts + "\n");
		sb.append("\n");
		sb.append("# the delay of two subsequent polling attempts, in milliseconds,\n");
		sb.append("# while waiting for the result of a posted transaction;\n");
		sb.append("# this delay is then increased by 10% at each subsequent attempt\n");
		sb.append("polling_delay = " + pollingDelay + "\n");
		sb.append("\n");
		sb.append("# the size of the cache for getRequest() method of the node\n");
		sb.append("request_cache_size = " + requestCacheSize + "\n");
		sb.append("\n");
		sb.append("# the size of the cache for getResponse() method of the node\n");
		sb.append("response_cache_size = " + responseCacheSize + "\n");
		sb.append("\n");
		sb.append("# the maximal amount of gas that a view transaction can consume\n");
		sb.append("max_gas_per_view_transaction = \"" + maxGasPerViewTransaction + "\"\n");

		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other != null && getClass() == other.getClass()) {
			var otherConfig = (LocalNodeConfigImpl) other;
			return dir.equals(otherConfig.dir) &&
				maxPollingAttempts == otherConfig.maxPollingAttempts &&
				pollingDelay == otherConfig.pollingDelay &&
				requestCacheSize == otherConfig.requestCacheSize &&
				responseCacheSize == otherConfig.responseCacheSize &&
				maxGasPerViewTransaction.equals(otherConfig.maxGasPerViewTransaction);
		}
		else
			return false;
	}

	@Override
	public String toString() {
		return toToml();
	}

	/**
	 * The builder of a configuration object.
	 */
	protected abstract static class ConfigBuilderImpl<T extends LocalNodeConfigBuilder<T>> implements LocalNodeConfigBuilder<T> {
		private Path dir = Paths.get("chain");
		private long maxPollingAttempts = 60;
		private long pollingDelay = 10;
		private int requestCacheSize = 1_000;
		private int responseCacheSize = 1_000;
		private BigInteger maxGasPerViewTransaction = BigInteger.valueOf(100_000_000);

		/**
		 * Creates a builder with default values for the properties.
		 */
		protected ConfigBuilderImpl() {}

		/**
		 * Creates a builder with properties initialized to those of the given configuration object.
		 * 
		 * @param config the configuration object
		 */
		protected ConfigBuilderImpl(LocalNodeConfig config) {
			setDir(config.getDir());
			setMaxPollingAttempts(config.getMaxPollingAttempts());
			setPollingDelay(config.getPollingDelay());
			setRequestCacheSize(config.getRequestCacheSize());
			setResponseCacheSize(config.getResponseCacheSize());
			setMaxGasPerViewTransaction(config.getMaxGasPerViewTransaction());
		}

		/**
		 * Creates a builder by reading the properties of the given TOML file and
		 * setting them for the corresponding fields of the builder.
		 * 
		 * @param toml the file
		 */
		protected ConfigBuilderImpl(Toml toml) {
			var dir = toml.getString("dir");
			if (dir != null)
				setDir(Paths.get(dir));

			var maxPollingAttempts = toml.getLong("max_polling_attempts");
			if (maxPollingAttempts != null)
				setMaxPollingAttempts(maxPollingAttempts);

			var pollingDelay = toml.getLong("polling_delay");
			if (pollingDelay != null)
				setPollingDelay(pollingDelay);

			var requestCacheSize = toml.getLong("request_cache_size");
			if (requestCacheSize != null)
				if (requestCacheSize < 0 || requestCacheSize > Integer.MAX_VALUE)
					throw new IllegalArgumentException("requestCacheSize must be non-negative and no larger than " + Integer.MAX_VALUE);
				else
					setRequestCacheSize((int) (long) requestCacheSize);

			var responseCacheSize = toml.getLong("response_cache_size");
			if (responseCacheSize < 0 || responseCacheSize > Integer.MAX_VALUE)
				throw new IllegalArgumentException("responseCacheSize must be non-negative and no larger than " + Integer.MAX_VALUE);
			else
				setResponseCacheSize((int) (long) responseCacheSize);

			var maxGasPerViewTransaction = toml.getString("max_gas_per_view_transaction");
			if (maxGasPerViewTransaction != null)
				setMaxGasPerViewTransaction(new BigInteger(maxGasPerViewTransaction));
		}

		@Override
		public T setMaxGasPerViewTransaction(BigInteger maxGasPerViewTransaction) {
			Objects.requireNonNull(maxGasPerViewTransaction, "maxGasPerViewTransaction cannot be null");
			if (maxGasPerViewTransaction.signum() < 0)
				throw new IllegalArgumentException("maxGasPerViewTransaction must be non-negative");

			this.maxGasPerViewTransaction = maxGasPerViewTransaction;
	
			return getThis();
		}

		@Override
		public T setDir(Path dir) {
			Objects.requireNonNull(dir, "dir cannot be null");
			this.dir = dir;
			return getThis();
		}

		@Override
		public T setMaxPollingAttempts(long maxPollingAttempts) {
			if (maxPollingAttempts <= 0)
				throw new IllegalArgumentException("maxPollingAttempts must be positive");

			this.maxPollingAttempts = maxPollingAttempts;
			return getThis();
		}

		@Override
		public T setPollingDelay(long pollingDelay) {
			if (pollingDelay < 0)
				throw new IllegalArgumentException("pollingDelay must non-negative");

			this.pollingDelay = pollingDelay;
			return getThis();
		}

		@Override
		public T setRequestCacheSize(int requestCacheSize) {
			if (requestCacheSize < 0)
				throw new IllegalArgumentException("requestCacheSize must non-negative");

			this.requestCacheSize = requestCacheSize;
			return getThis();
		}

		@Override
		public T setResponseCacheSize(int responseCacheSize) {
			if (responseCacheSize < 0)
				throw new IllegalArgumentException("responseCacheSize must non-negative");

			this.responseCacheSize = responseCacheSize;
			return getThis();
		}

		/**
		 * Standard design pattern. See http://www.angelikalanger.com/GenericsFAQ/FAQSections/ProgrammingIdioms.html#FAQ205
		 */
		protected abstract T getThis();

		/**
		 * Loads the TOML file at the given path.
		 * 
		 * @param path the path
		 * @return the file
		 * @throws FileNotFoundException if {@code path} cannot be found
		 */
		protected static Toml readToml(Path path) throws FileNotFoundException {
			try {
				return new Toml().read(path.toFile());
			}
			catch (RuntimeException e) {
				// the toml4j library wraps the FileNotFoundException inside a RuntimeException...
				Throwable cause = e.getCause();
				if (cause instanceof FileNotFoundException)
					throw (FileNotFoundException) cause;
				else
					throw e;
			}
		}
	}
}