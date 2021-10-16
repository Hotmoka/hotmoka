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

package io.hotmoka.memory;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.local.Config;

/**
 * The configuration of a blockchain on disk memory.
 */
@Immutable
public class MemoryBlockchainConfig extends Config {

	/**
	 * The number of transactions that fit inside a block.
	 * It defaults to 5.
	 */
	public final int transactionsPerBlock;

	/**
	 * Full constructor for the builder pattern.
	 * 
	 * @param transactionsPerBlock the number of transactions that fit inside a block.
	 *                             It defaults to 5.
	 */
	protected MemoryBlockchainConfig(io.hotmoka.local.Config superConfig, int transactionsPerBlock) {
		super(superConfig);

		this.transactionsPerBlock = transactionsPerBlock;
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class Builder extends io.hotmoka.local.Config.Builder<Builder> {

		/**
		 * The number of transactions that fit inside a block.
		 */
		private int transactionsPerBlock = 5;

		@Override
		public MemoryBlockchainConfig build() {
			return new MemoryBlockchainConfig(super.build(), transactionsPerBlock);
		}

		/**
		 * Sets the number of transactions that fit inside a block.
		 * It defaults to 5.
		 * 
		 * @param transactionsPerBlock the number of transactions that fit inside a block
		 * @return this builder
		 */
		public Builder setTransactionsPerBlock(int transactionsPerBlock) {
			this.transactionsPerBlock = transactionsPerBlock;
			return this;
		}

		@Override
		protected Builder getThis() {
			return this;
		}
	}
}