package io.hotmoka.takamaka;

import io.hotmoka.beans.annotations.Immutable;

/**
 * The configuration of a Takamaka blockchain.
 */
@Immutable
public class TakamakaBlockchainConfig extends io.takamaka.code.engine.Config {

	/**
	 * The port of the Takamaka process, on localhost. Defaults to 60006.
	 */
	public final int takamakaPort;

	/**
	 * The maximal number of connection attempts to the Takamaka process during ping.
	 * Defaults to 20.
	 */
	public final int maxPingAttempts;

	/**
	 * The delay between two successive ping attempts, in milliseconds. Defaults to 200.
	 */
	public final int pingDelay;

	/**
	 * Full constructor for the builder pattern.
	 */
	protected TakamakaBlockchainConfig(io.takamaka.code.engine.Config superConfig, int takamakaPort, int maxPingAttemps, int pingDelay) {
		super(superConfig);

		this.takamakaPort = takamakaPort;
		this.maxPingAttempts = maxPingAttemps;
		this.pingDelay = pingDelay;
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class Builder extends io.takamaka.code.engine.Config.Builder<Builder> {
		private int takamakaPort = 60006;
		private int maxPingAttempts = 20;
		private int pingDelay = 200;

		/**
		 * Sets the port of the Takamaka process, on localhost. Defaults to 60006.
		 * 
		 * @param takamakaPort the port
		 * @return this builder
		 */
		public Builder setTakamakaPort(int takamakaPort) {
			this.takamakaPort = takamakaPort;
			return this;
		}

		/**
		 * Sets the maximal number of connection attempts to the Tendermint process during ping.
		 * Defaults to 20.
		 * 
		 * @param maxPingAttempts the max number of attempts
		 * @return this builder
		 */
		public Builder setMaxPingAttempts(int maxPingAttempts) {
			this.maxPingAttempts = maxPingAttempts;
			return this;
		}

		/**
		 * Sets the delay between two successive ping attempts, in milliseconds. Defaults to 200.
		 * 
		 * @param pingDelay the delay
		 * @return this builder
		 */
		public Builder setPingDelay(int pingDelay) {
			this.pingDelay = pingDelay;
			return this;
		}

		@Override
		public TakamakaBlockchainConfig build() {
			return new TakamakaBlockchainConfig(super.build(), takamakaPort, maxPingAttempts, pingDelay);
		}

		@Override
		protected Builder getThis() {
			return this;
		}
	}
}