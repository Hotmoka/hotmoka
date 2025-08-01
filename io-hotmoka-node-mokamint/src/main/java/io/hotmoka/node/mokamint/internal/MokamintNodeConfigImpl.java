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

package io.hotmoka.node.mokamint.internal;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import com.moandjiezana.toml.Toml;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.local.AbstractLocalNodeConfig;
import io.hotmoka.node.mokamint.api.MokamintNodeConfig;
import io.hotmoka.node.mokamint.api.MokamintNodeConfigBuilder;

/**
 * The configuration of a node based on the Mokamint proof of space engine.
 */
@Immutable
public class MokamintNodeConfigImpl extends AbstractLocalNodeConfig<MokamintNodeConfig, MokamintNodeConfigBuilder> implements MokamintNodeConfig {

	/**
	 * The depth of the indexing, that is, the number of uppermost blocks
	 * for which indexing supporting data is maintained. The larger this number,
	 * the more resilient is indexing to large history changes, but higher is
	 * its computational cost and database usage. A negative value means that supporting data
	 * is kept forever, it is never deleted, which protects completely from history changes.
	 */
	public final long indexingDepth;

	/**
	 * The pausing time (in milliseconds) from an indexing iteration to the
	 * next indexing iteration. Reducing this number will make indexing more
	 * reactive to changes in the store, at an increased computational cost.
	 */
	public final long indexingPause;

	/**
	 * Creates a configuration object from its builder.
	 * 
	 * @param the builder
	 */
	private MokamintNodeConfigImpl(MokamintNodeConfigBuilderImpl builder) {
		super(builder);

		this.indexingDepth = builder.indexingDepth;
		this.indexingPause = builder.indexingPause;
	}

	@Override
	public long getIndexingDepth() {
		return indexingDepth;
	}

	@Override
	public long getIndexingPause() {
		return indexingPause;
	}

	@Override
	public boolean equals(Object other) {
		return super.equals(other) && other instanceof MokamintNodeConfigImpl mnci
				&& indexingDepth == mnci.indexingDepth && indexingPause == mnci.indexingPause;
	}

	@Override
	public String toToml() {
		var sb = new StringBuilder(super.toToml());

		sb.append("\n");
		sb.append("# the number of uppermost blockchain blocks for which supporting indexing data is kept;\n");
		sb.append("# a higher value makes indexing more resilient to history changes, for a higher\n");
		sb.append("# computational cost; a negative value means that supporting indexing data is kept forever,\n");
		sb.append("# which completely protects against history changes\n");
		sb.append("indexing_depth = " + indexingDepth + "\n");

		sb.append("\n");
		sb.append("# the length of the pause, in milliseconds, between successive indexing iterations\n");
		sb.append("indexing_pause = " + indexingPause + "\n");

		return sb.toString();
	}

	@Override
	public MokamintNodeConfigBuilder toBuilder() {
		return new MokamintNodeConfigBuilderImpl(this);
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class MokamintNodeConfigBuilderImpl extends AbstractLocalNodeConfigBuilder<MokamintNodeConfig, MokamintNodeConfigBuilder> implements MokamintNodeConfigBuilder {
		private long indexingDepth = 1024L;
		private long indexingPause = 20_000L; // 20 seconds

		/**
		 * Creates a builder with default values for the properties.
		 */
		public MokamintNodeConfigBuilderImpl() {}

		/**
		 * Creates a builder by reading the properties of the given TOML file and sets them for
		 * the corresponding fields of this builder.
		 * 
		 * @param toml the file
		 * @throws FileNotFoundException if the file cannot be found
		 */
		public MokamintNodeConfigBuilderImpl(Path toml) throws FileNotFoundException {
			this(readToml(toml));
		}

		/**
		 * Creates a builder by reading the properties of the given TOML file and sets them for
		 * the corresponding fields of this builder.
		 * 
		 * @param toml the file
		 */
		private MokamintNodeConfigBuilderImpl(Toml toml) {
			super(toml);

			var indexingDepth = toml.getLong("indexing_depth");
			if (indexingDepth != null)
				setIndexingDepth(indexingDepth);

			var indexingPause = toml.getLong("indexing_pause");
			if (indexingPause != null)
				setIndexingPause(indexingPause);
		}

		/**
		 * Creates a builder with properties initialized to those of the given configuration object.
		 * 
		 * @param config the configuration object
		 */
		private MokamintNodeConfigBuilderImpl(MokamintNodeConfigImpl config) {
			super(config);

			setIndexingDepth(config.indexingDepth);
			setIndexingPause(config.indexingPause);
		}

		@Override
		public MokamintNodeConfigBuilder setIndexingDepth(long indexingDepth) {
			this.indexingDepth = indexingDepth;
			return getThis();
		}

		@Override
		public MokamintNodeConfigBuilder setIndexingPause(long indexingPause) {
			if (indexingPause < 0L)
				throw new IllegalArgumentException("indexingPause must be non-negative");

			this.indexingPause = indexingPause;
			return getThis();
		}

		@Override
		public MokamintNodeConfig build() {
			return new MokamintNodeConfigImpl(this);
		}

		@Override
		protected MokamintNodeConfigBuilder getThis() {
			return this;
		}
	}
}