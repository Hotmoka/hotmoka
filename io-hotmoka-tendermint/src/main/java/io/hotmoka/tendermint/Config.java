package io.hotmoka.tendermint;

import io.hotmoka.beans.annotations.Immutable;

/**
 * The configuration of a Tendermint blockchain.
 */
@Immutable
public class Config extends io.takamaka.code.engine.Config {

	/**
	 * The port of the Tendermint process. This will be spawned on localhost.
	 * Defaults to 26657.
	 */
	public final int tendermintPort;

	/**
	 * The port of the ABCI application. This will be spawned on localhost.
	 * Defaults to 26658.
	 */
	public final int abciPort;

	/**
	 * The maximal number of connection attempts to the Tendermint process during ping.
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
	protected Config(io.takamaka.code.engine.Config superConfig, int tendermintPort, int abciPort, int maxPingAttemps, int pingDelay) {
		super(superConfig);

		this.tendermintPort = tendermintPort;
		this.abciPort = abciPort;
		this.maxPingAttempts = maxPingAttemps;
		this.pingDelay = pingDelay;
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class Builder extends io.takamaka.code.engine.Config.Builder {
		private int tendermintPort = 26657;
		private int abciPort = 26658;
		private int maxPingAttempts = 20;
		private int pingDelay = 200;

		/**
		 * Sets the port of the Tendermint process. This will be spawned on localhost.
		 * Defaults to 26657.
		 * 
		 * @param tendermintPort the port
		 * @return this builder
		 */
		public Builder setTendermintPort(int tendermintPort) {
			this.tendermintPort = tendermintPort;
			return this;
		}

		/**
		 * Sets the port of the ABCI application. This will be spawned on localhost.
		 * Defaults to 26658.
		 * 
		 * @param abciPort the port
		 * @return this builder
		 */
		public Builder setAbciPort(int abciPort) {
			this.abciPort = abciPort;
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
		public Config build() {
			return new Config(super.build(), tendermintPort, abciPort, maxPingAttempts, pingDelay);
		}
	}
}