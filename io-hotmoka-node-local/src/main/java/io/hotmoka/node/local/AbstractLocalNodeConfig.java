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

package io.hotmoka.node.local;

import com.moandjiezana.toml.Toml;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.local.api.LocalNodeConfig;
import io.hotmoka.node.local.api.LocalNodeConfigBuilder;
import io.hotmoka.node.local.internal.LocalNodeConfigImpl;

/**
 * Partial implementation of the configuration of a local node.
 * 
 * @param <C> the concrete type of the configuration
 * @param <B> the concrete type of the builder
 */
@Immutable
public abstract class AbstractLocalNodeConfig<C extends LocalNodeConfig<C,B>, B extends LocalNodeConfigBuilder<C,B>> extends LocalNodeConfigImpl<C,B> {

	/**
	 * Creates a new configuration object from its builder.
	 * 
	 * @param builder the builder
	 */
	protected AbstractLocalNodeConfig(AbstractLocalNodeConfigBuilder<C,B> builder) {
		super(builder);
	}

	/**
	 * The builder of a configuration object.
	 * 
	 * @param <C> the concrete type of the configuration
	 * @param <B> the concrete type of the builder
	 */
	protected abstract static class AbstractLocalNodeConfigBuilder<C extends LocalNodeConfig<C,B>, B extends LocalNodeConfigBuilder<C,B>> extends LocalNodeConfigBuilderImpl<C,B> {

		/**
		 * Creates a builder with default values for the properties.
		 */
		protected AbstractLocalNodeConfigBuilder() {}

		/**
		 * Creates a builder by reading the properties of the given TOML file and sets them for
		 * the corresponding fields of this builder.
		 * 
		 * @param toml the file
		 */
		protected AbstractLocalNodeConfigBuilder(Toml toml) {
			super(toml);
		}

		/**
		 * Creates a builder with properties initialized to those of the given configuration object.
		 * 
		 * @param config the configuration object
		 */
		protected AbstractLocalNodeConfigBuilder(LocalNodeConfig<C,B> config) {
			super(config);
		}
	}
}