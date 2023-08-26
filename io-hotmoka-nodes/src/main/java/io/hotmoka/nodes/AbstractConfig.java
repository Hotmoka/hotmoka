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

package io.hotmoka.nodes;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.nodes.internal.ConsensusConfigImpl;

/**
 * The configuration of a Hotmoka node. Nodes of the same network must agree
 * on this data in order to achieve consensus.
 */
@Immutable
public abstract class AbstractConfig extends ConsensusConfigImpl {

	/**
	 * Full constructor for the builder pattern.
	 * 
	 * @param builder the builder where information is extracted from
	 */
	protected AbstractConfig(AbstractConfigBuilder<?> builder) {
		super(builder);
	}
}