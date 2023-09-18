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

package io.hotmoka.node.disk.api;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.local.api.LocalNodeConfigBuilder;

/**
 * The builder of a configuration of a node on disk memory.
 */
@Immutable
public interface DiskNodeConfigBuilder extends LocalNodeConfigBuilder<DiskNodeConfigBuilder> {

	/**
	 * Sets the number of transactions that fit inside a block.
	 * It defaults to 5.
	 * 
	 * @param transactionsPerBlock the number of transactions that fit inside a block
	 * @return this builder
	 */
	DiskNodeConfigBuilder setTransactionsPerBlock(int transactionsPerBlock);

	@Override
	DiskNodeConfig build(); // TODO: remove?
}