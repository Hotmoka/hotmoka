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
	 * Creates a new configuration object from its builder.
	 * 
	 * @param the builder
	 */
	private MokamintNodeConfigImpl(MokamintNodeConfigBuilderImpl builder) {
		super(builder);
	}

	@Override
	public boolean equals(Object other) {
		return super.equals(other);
	}

	@Override
	public String toToml() {
		var sb = new StringBuilder(super.toToml());

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
		}

		/**
		 * Creates a builder with properties initialized to those of the given configuration object.
		 * 
		 * @param config the configuration object
		 */
		private MokamintNodeConfigBuilderImpl(MokamintNodeConfigImpl config) {
			super(config);
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