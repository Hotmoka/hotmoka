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

package io.hotmoka.node.internal;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.SimpleValidatorsConsensusConfig;
import io.hotmoka.node.api.SimpleValidatorsConsensusConfigBuilder;

/**
 * Concrete implementation of the consensus parameters of a Hotmoka node with validators.
 * This information is typically contained in the manifest of the node.
 */
@Immutable
public class SimpleValidatorsConsensusConfigImpl extends ValidatorsConsensusConfigImpl<SimpleValidatorsConsensusConfig, SimpleValidatorsConsensusConfigBuilder> implements SimpleValidatorsConsensusConfig {

	/**
	 * Full constructor for the builder pattern.
	 * 
	 * @param builder the builder where information is extracted from
	 */
	private SimpleValidatorsConsensusConfigImpl(SimpleValidatorsConsensusConfigBuilderImpl builder) {
		super(builder);
	}

	@Override
	public SimpleValidatorsConsensusConfigBuilder toBuilder() {
		return new SimpleValidatorsConsensusConfigBuilderImpl(this);
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class SimpleValidatorsConsensusConfigBuilderImpl extends ValidatorsConsensusConfigBuilderImpl<SimpleValidatorsConsensusConfig, SimpleValidatorsConsensusConfigBuilder> implements SimpleValidatorsConsensusConfigBuilder {

		/**
		 * Creates a builder with default values for the properties.
		 * 
		 * @throws NoSuchAlgorithmException if some signature algorithm is not available
		 */
		public SimpleValidatorsConsensusConfigBuilderImpl() throws NoSuchAlgorithmException {
		}

		/**
		 * Creates a builder by reading the properties of the given TOML file and sets them for
		 * the corresponding fields of this builder.
		 * 
		 * @param toml the file
		 * @throws FileNotFoundException if the file cannot be found
		 * @throws NoSuchAlgorithmException if some signature algorithm in the TOML file is not available
		 */
		public SimpleValidatorsConsensusConfigBuilderImpl(Path toml) throws FileNotFoundException, NoSuchAlgorithmException {
			super(readToml(toml));
		}

		/**
		 * Creates a builder with properties initialized to those of the given configuration object.
		 * 
		 * @param config the configuration object
		 */
		private SimpleValidatorsConsensusConfigBuilderImpl(SimpleValidatorsConsensusConfig config) {
			super(config);
		}

		@Override
		protected SimpleValidatorsConsensusConfigBuilder getThis() {
			return this;
		}

		@Override
		public SimpleValidatorsConsensusConfig build() {
			return new SimpleValidatorsConsensusConfigImpl(this);
		}
	}
}