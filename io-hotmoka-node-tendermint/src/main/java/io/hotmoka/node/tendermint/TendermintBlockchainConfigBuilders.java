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

package io.hotmoka.node.tendermint;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import io.hotmoka.node.tendermint.api.TendermintBlockchainConfigBuilder;
import io.hotmoka.node.tendermint.internal.TendermintBlockchainConfigImpl.TendermintBlockchainConfigBuilderImpl;

/**
 * Providers of configuration object builders of a blockchain based on Tendermint.
 */
public abstract class TendermintBlockchainConfigBuilders {

	private TendermintBlockchainConfigBuilders() {}

	/**
	 * Creates a builder containing default data.
	 * 
	 * @return the builder
	 */
	public static TendermintBlockchainConfigBuilder defaults() {
		return new TendermintBlockchainConfigBuilderImpl();
	}

	/**
	 * Creates a builder from the given TOML configuration file.
	 * The resulting builder will contain the information in the file,
	 * and use defaults for the data not contained in the file.
	 * 
	 * @param path the path to the TOML file
	 * @return the builder
	 * @throws FileNotFoundException if {@code path} cannot be found
	 */
	public static TendermintBlockchainConfigBuilder load(Path path) throws FileNotFoundException {
		return new TendermintBlockchainConfigBuilderImpl(path);
	}
}