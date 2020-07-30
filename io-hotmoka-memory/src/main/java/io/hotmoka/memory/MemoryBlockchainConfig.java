package io.hotmoka.memory;

import io.hotmoka.beans.annotations.Immutable;

/**
 * The configuration of a blockchain on disk memory.
 */
@Immutable
public class MemoryBlockchainConfig extends io.takamaka.code.engine.Config {

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
	protected MemoryBlockchainConfig(io.takamaka.code.engine.Config superConfig, int transactionsPerBlock) {
		super(superConfig);

		this.transactionsPerBlock = transactionsPerBlock;
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class Builder extends io.takamaka.code.engine.Config.Builder<Builder> {

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