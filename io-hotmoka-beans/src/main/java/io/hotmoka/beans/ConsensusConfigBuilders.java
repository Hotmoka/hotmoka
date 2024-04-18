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

package io.hotmoka.beans;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import io.hotmoka.beans.api.nodes.ConsensusConfigBuilder;
import io.hotmoka.crypto.Base64ConversionException;

/**
 * Providers of consensus configurations.
 */
public abstract class ConsensusConfigBuilders {

	private ConsensusConfigBuilders() {}

	private static class MyConsensusConfig extends AbstractConsensusConfig<MyConsensusConfig, MyConsensusConfigBuilder> {
		
		/**
		 * Full constructor for the builder pattern.
		 * 
		 * @param builder the builder where information is extracted from
		 */
		private MyConsensusConfig(MyConsensusConfigBuilder builder) {
			super(builder);
		}

		@Override
		public MyConsensusConfigBuilder toBuilder() {
			return new MyConsensusConfigBuilder(this);
		}
	}

	/**
	 * The builder of consensus configurations, according to the builder pattern.
	 */
	private static class MyConsensusConfigBuilder extends AbstractConsensusConfigBuilder<MyConsensusConfig, MyConsensusConfigBuilder> {

		private MyConsensusConfigBuilder() throws NoSuchAlgorithmException {
		}

		private MyConsensusConfigBuilder(Path path) throws NoSuchAlgorithmException, FileNotFoundException, InvalidKeyException, InvalidKeySpecException, Base64ConversionException {
			super(readToml(path));
		}

		private MyConsensusConfigBuilder(MyConsensusConfig config) {
			super(config);
		}

		@Override
		public MyConsensusConfig build() {
			return new MyConsensusConfig(this);
		}

		@Override
		protected MyConsensusConfigBuilder getThis() {
			return this;
		}
	}

	/**
	 * Creates a builder containing default data.
	 * 
	 * @return the builder
	 * @throws NoSuchAlgorithmException if some hashing algorithm used in the default configuration is not available
	 */
	public static ConsensusConfigBuilder<?,?> defaults() throws NoSuchAlgorithmException {
		return new MyConsensusConfigBuilder();
	}

	/**
	 * Creates a builder from the given TOML configuration file.
	 * The resulting builder will contain the information in the file,
	 * and use defaults for the data not contained in the file.
	 * 
	 * @param path the path to the TOML file
	 * @return the builder
	 * @throws FileNotFoundException if {@code path} cannot be found
	 * @throws NoSuchAlgorithmException if some hashing algorithm cannot be found
	 * @throws Base64ConversionException if some public key in the TOML file is not correctly Base64-encoded
	 * @throws InvalidKeySpecException if the specification of some public key in the TOML file is illegal
	 * @throws InvalidKeyException if some public key in the TOML file is invalid
	 */
	public static ConsensusConfigBuilder<?,?> load(Path path) throws NoSuchAlgorithmException, FileNotFoundException, InvalidKeyException, InvalidKeySpecException, Base64ConversionException {
		return new MyConsensusConfigBuilder(path);
	}
}