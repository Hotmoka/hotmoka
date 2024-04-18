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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.node.api.SimpleConsensusConfig;
import io.hotmoka.node.api.SimpleConsensusConfigBuilder;

/**
 * Concrete implementation of the consensus parameters of a Hotmoka node. This information
 * is typically contained in the manifest of the node.
 */
@Immutable
public class SimpleConsensusConfigImpl extends ConsensusConfigImpl<SimpleConsensusConfig, SimpleConsensusConfigBuilder> implements SimpleConsensusConfig {

	/**
	 * Full constructor for the builder pattern.
	 * 
	 * @param builder the builder where information is extracted from
	 */
	private SimpleConsensusConfigImpl(SimpleConsensusConfigBuilderImpl builder) {
		super(builder);
	}

	@Override
	public SimpleConsensusConfigBuilder toBuilder() {
		return new SimpleConsensusConfigBuilderImpl(this);
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class SimpleConsensusConfigBuilderImpl extends ConsensusConfigBuilderImpl<SimpleConsensusConfig, SimpleConsensusConfigBuilder> implements SimpleConsensusConfigBuilder {

		/**
		 * Creates a builder with default values for the properties.
		 * 
		 * @throws NoSuchAlgorithmException if some signature algorithm is not available
		 */
		public SimpleConsensusConfigBuilderImpl() throws NoSuchAlgorithmException {
		}

		/**
		 * Creates a builder by reading the properties of the given TOML file and sets them for
		 * the corresponding fields of this builder.
		 * 
		 * @param toml the file
		 * @throws FileNotFoundException if the file cannot be found
		 * @throws NoSuchAlgorithmException if some cryptographic algorithm in the TOML file is not available
		 * @throws Base64ConversionException if some public key in the TOML file is not correctly Base64-encoded
		 * @throws InvalidKeySpecException if the specification of some public key in the TOML file is illegal
		 * @throws InvalidKeyException if some public key in the TOML file is invalid
		 */
		public SimpleConsensusConfigBuilderImpl(Path toml) throws FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, Base64ConversionException, InvalidKeyException {
			super(readToml(toml));
		}

		/**
		 * Creates a builder with properties initialized to those of the given configuration object.
		 * 
		 * @param config the configuration object
		 */
		private SimpleConsensusConfigBuilderImpl(SimpleConsensusConfig config) {
			super(config);
		}

		@Override
		protected SimpleConsensusConfigBuilder getThis() {
			return this;
		}

		@Override
		public SimpleConsensusConfig build() {
			return new SimpleConsensusConfigImpl(this);
		}
	}
}