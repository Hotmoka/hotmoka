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

package io.hotmoka.node.disk.internal;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import com.moandjiezana.toml.Toml;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.disk.api.DiskNodeConfig;
import io.hotmoka.node.disk.api.DiskNodeConfigBuilder;
import io.hotmoka.node.local.AbstractLocalNodeConfig;

/**
 * The configuration of a node in disk memory.
 */
@Immutable
public class DiskNodeConfigImpl extends AbstractLocalNodeConfig<DiskNodeConfig, DiskNodeConfigBuilder> implements DiskNodeConfig {

	/**
	 * The number of transactions that fit inside a block. It defaults to 10.
	 */
	public final int transactionsPerBlock;

	/**
	 * Creates a new configuration object from its builder.
	 * 
	 * @param the builder
	 */
	private DiskNodeConfigImpl(DiskNodeConfigBuilderImpl builder) {
		super(builder);

		this.transactionsPerBlock = builder.transactionsPerBlock;
	}

	@Override
	public int getTransactionsPerBlock() {
		return transactionsPerBlock;
	}

	@Override
	public boolean equals(Object other) {
		return super.equals(other) && transactionsPerBlock == ((DiskNodeConfigImpl) other).transactionsPerBlock;
	}

	@Override
	public String toToml() {
		var sb = new StringBuilder(super.toToml());

		sb.append("\n");
		sb.append("# the number of transactions that fit inside a block\n");
		sb.append("transactions_per_block = " + transactionsPerBlock + "\n");

		return sb.toString();
	}

	@Override
	public DiskNodeConfigBuilder toBuilder() {
		return new DiskNodeConfigBuilderImpl(this);
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class DiskNodeConfigBuilderImpl extends AbstractLocalNodeConfigBuilder<DiskNodeConfig, DiskNodeConfigBuilder> implements DiskNodeConfigBuilder {

		/**
		 * The number of transactions that fit inside a block.
		 */
		private int transactionsPerBlock = 10;

		/**
		 * Creates a builder with default values for the properties.
		 */
		public DiskNodeConfigBuilderImpl() {}

		/**
		 * Creates a builder by reading the properties of the given TOML file and sets them for
		 * the corresponding fields of this builder.
		 * 
		 * @param toml the file
		 * @throws FileNotFoundException if the file cannot be found
		 */
		public DiskNodeConfigBuilderImpl(Path toml) throws FileNotFoundException {
			this(readToml(toml));
		}

		/**
		 * Creates a builder by reading the properties of the given TOML file and sets them for
		 * the corresponding fields of this builder.
		 * 
		 * @param toml the file
		 */
		private DiskNodeConfigBuilderImpl(Toml toml) {
			super(toml);
		
			var transactionsPerBlock = toml.getLong("transactions_per_block");
			if (transactionsPerBlock != null)
				setTransactionsPerBlock(transactionsPerBlock);
		}

		/**
		 * Creates a builder with properties initialized to those of the given configuration object.
		 * 
		 * @param config the configuration object
		 */
		private DiskNodeConfigBuilderImpl(DiskNodeConfigImpl config) {
			super(config);

			setTransactionsPerBlock(config.transactionsPerBlock);
		}

		@Override
		public DiskNodeConfigBuilder setTransactionsPerBlock(int transactionsPerBlock) {
			if (transactionsPerBlock <= 0L)
				throw new IllegalArgumentException("transactionsPerBlock cannot be negative");

			this.transactionsPerBlock = transactionsPerBlock;
			return getThis();
		}

		private DiskNodeConfigBuilder setTransactionsPerBlock(long transactionsPerBlock) {
			if (transactionsPerBlock <= 0L || transactionsPerBlock > Integer.MAX_VALUE)
				throw new IllegalArgumentException("transactionsPerBlock must be between 0 and " + Integer.MAX_VALUE + " inclusive");

			this.transactionsPerBlock = (int) transactionsPerBlock;
			return getThis();
		}

		@Override
		public DiskNodeConfig build() {
			return new DiskNodeConfigImpl(this);
		}

		@Override
		protected DiskNodeConfigBuilder getThis() {
			return this;
		}
	}
}