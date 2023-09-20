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

package io.hotmoka.node.api;

import io.hotmoka.annotations.Immutable;

/**
 * A specification of the consensus parameters of a Hotmoka node that uses validators.
 * This information is typically contained in the manifest of the node.
 * 
 * @param <C> the concrete type of the configuration
 * @param <B> the concrete type of the builder
 */
@Immutable
public interface ValidatorsConsensusConfig<C extends ValidatorsConsensusConfig<C,B>, B extends ValidatorsConsensusConfigBuilder<C,B>> extends ConsensusConfig<C,B> {

	/**
	 * Yields the amount of validators' rewards that gets staked. The rest is sent to the validators immediately.
	 * 1000000 = 1%.
	 * 
	 * @return the amount of validators' rewards that gets staked; 1000000 = 1%
	 */
	int getPercentStaked();

	/**
	 * Yields extra tax paid when a validator acquires the shares of another validator
	 * (in percent of the offer cost). 1000000 = 1%.
	 * 
	 * @return the extra tax paid when a validator acquires the shares of another validator
	 *         (in percent of the offer cost). 1000000 = 1%
	 */
	int getBuyerSurcharge();

	/**
	 * Yields the percent of stake that gets slashed for each misbehaving validator. 1000000 means 1%.
	 * 
	 * @return the percent of stake that gets slashed for each misbehaving validator. 1000000 means 1%
	 */
	int getSlashingForMisbehaving();

	/**
	 * Yields the percent of stake that gets slashed for validators that do not behave
	 * (or do not vote). 1000000 means 1%.
	 * 
	 * @return the percent of stake that gets slashed for validators that do not behave
	 *         (or do not vote). 1000000 means 1%
	 */
	int getSlashingForNotBehaving();
}