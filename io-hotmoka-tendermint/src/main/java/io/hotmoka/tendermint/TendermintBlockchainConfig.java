package io.hotmoka.tendermint;

import java.nio.file.Path;

import io.hotmoka.beans.annotations.Immutable;

/**
 * The configuration of a Tendermint blockchain.
 */
@Immutable
public class TendermintBlockchainConfig extends io.takamaka.code.engine.Config {

	/**
	 * The directory that contains the Tendermint configuration that must be cloned
	 * if a brand new Tendermint blockchain is created.
	 * That configuration will then be used for the execution of Tendermint.
	 * This might be {@code null}, in which case a default Tendermint configuration is created,
	 * with the same node as single validator. It defaults to {@code null}.
	 */
	public final Path tendermintConfigurationToClone;

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
	protected TendermintBlockchainConfig(io.takamaka.code.engine.Config superConfig, Path tendermintConfigurationToClone,
			int tendermintPort, int abciPort, int maxPingAttemps, int pingDelay) {

		super(superConfig);

		this.tendermintConfigurationToClone = tendermintConfigurationToClone;
		this.tendermintPort = tendermintPort;
		this.abciPort = abciPort;
		this.maxPingAttempts = maxPingAttemps;
		this.pingDelay = pingDelay;
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class Builder extends io.takamaka.code.engine.Config.Builder<Builder> {
		private int tendermintPort = 26657;
		private int abciPort = 26658;
		private int maxPingAttempts = 20;
		private int pingDelay = 200;
		private Path tendermintConfigurationToClone;

		/**
		 * Sets the directory that contains the Tendermint configuration that must be cloned
		 * if a brand new Tendermint blockchain is created.
		 * That configuration will then be used for the execution of Tendermint.
		 * This might be {@code null}, in which case a default Tendermint configuration is created,
		 * with the same node as single validator. It defaults to {@code null}.
		 * 
		 * @param tendermintConfigurationToClone the directory of the Tendermint configuration
		 *                                       to clone and use for Tendermint
		 * @return this builder
		 */
		public Builder setTendermintConfigurationToClone(Path tendermintConfigurationToClone) {
			this.tendermintConfigurationToClone = tendermintConfigurationToClone;

			return this;
		}

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
		public TendermintBlockchainConfig build() {
			return new TendermintBlockchainConfig(super.build(), tendermintConfigurationToClone, tendermintPort, abciPort, maxPingAttempts, pingDelay);
		}

		@Override
		protected Builder getThis() {
			return this;
		}
	}
}