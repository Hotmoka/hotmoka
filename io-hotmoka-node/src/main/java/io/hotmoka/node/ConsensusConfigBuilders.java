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

package io.hotmoka.node;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.node.api.ConsensusConfig;
import io.hotmoka.node.api.ConsensusConfigBuilder;

/**
 * Providers of consensus configuration builders.
 */
public final class ConsensusConfigBuilders {

	private ConsensusConfigBuilders() {}

	private static class MyConsensusConfigBuilder extends AbstractConfigBuilder<MyConsensusConfigBuilder> {

		private MyConsensusConfigBuilder() throws NoSuchAlgorithmException {}

		private MyConsensusConfigBuilder(SignatureAlgorithm<SignedTransactionRequest> signature) {
			super(signature);
		}

		private MyConsensusConfigBuilder(Path path) throws FileNotFoundException, NoSuchAlgorithmException {
			super(readToml(path));
		}

		@Override
		public ConsensusConfig build() {
			return new AbstractConfig(this) {

				@Override
				public ConsensusConfigBuilder<?> toBuilder() {
					return fill(new MyConsensusConfigBuilder(signature));
				}
			};
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
	 * @throws NoSuchAlgorithmException if some signature algorithm is not available
	 */
	public static ConsensusConfigBuilder<?> defaults() throws NoSuchAlgorithmException {
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
	 * @throws NoSuchAlgorithmException if some signature algorithm in the TOML file is not available
	 */
	public static ConsensusConfigBuilder<?> load(Path path) throws FileNotFoundException, NoSuchAlgorithmException {
		return new MyConsensusConfigBuilder(path);
	}
}