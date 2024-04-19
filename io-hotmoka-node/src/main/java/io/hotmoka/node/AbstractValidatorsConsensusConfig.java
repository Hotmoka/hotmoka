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

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.nodes.ValidatorsConsensusConfig;
import io.hotmoka.node.api.nodes.ValidatorsConsensusConfigBuilder;
import io.hotmoka.node.internal.nodes.ValidatorsConsensusConfigImpl;

/**
 * Implementation of the consensus parameters of a Hotmoka node that uses validators.
 * This information is typically contained in the manifest of the node.
 * 
 * @param <C> the concrete type of the configuration
 * @param <B> the concrete type of the builder
 */
@Immutable
public abstract class AbstractValidatorsConsensusConfig<C extends ValidatorsConsensusConfig<C,B>, B extends ValidatorsConsensusConfigBuilder<C,B>> extends ValidatorsConsensusConfigImpl<C,B> {

	/**
	 * Full constructor for the builder pattern.
	 * 
	 * @param builder the builder where information is extracted from
	 */
	protected AbstractValidatorsConsensusConfig(AbstractValidatorsConsensusConfigBuilder<C,B> builder) {
		super(builder);
	}
}