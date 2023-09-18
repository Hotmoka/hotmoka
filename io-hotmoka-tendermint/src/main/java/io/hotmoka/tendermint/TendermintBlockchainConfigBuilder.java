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

package io.hotmoka.tendermint;

import java.nio.file.Path;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.local.api.LocalNodeConfigBuilder;

/**
 * The builder of a configuration of a Tendermint blockchain.
 */
@Immutable
public interface TendermintBlockchainConfigBuilder extends LocalNodeConfigBuilder<TendermintBlockchainConfigBuilder> {

	/**
	 * Sets the directory that contains the Tendermint configuration that must be cloned
	 * if a brand new Tendermint blockchain is created.
	 * That configuration will then be used for the execution of Tendermint.
	 * This might be {@code null}, in which case a default Tendermint configuration is created,
	 * with the same node as single validator.
	 * 
	 * @param tendermintConfigurationToClone the directory of the Tendermint configuration
	 *                                       to clone and use for Tendermint
	 * @return this builder
	 */
	TendermintBlockchainConfigBuilder setTendermintConfigurationToClone(Path tendermintConfigurationToClone);

	/**
	 * Sets the maximal number of connection attempts to the Tendermint process during ping.
	 * 
	 * @param maxPingAttempts the max number of attempts
	 * @return this builder
	 */
	TendermintBlockchainConfigBuilder setMaxPingAttempts(int maxPingAttempts);

	/**
	 * Sets the delay between two successive ping attempts, in milliseconds. Defaults to 200.
	 * 
	 * @param pingDelay the delay
	 * @return this builder
	 */
	TendermintBlockchainConfigBuilder setPingDelay(int pingDelay);

	@Override
	TendermintBlockchainConfig build(); // TODO: remove?
}