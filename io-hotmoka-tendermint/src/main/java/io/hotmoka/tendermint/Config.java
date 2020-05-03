package io.hotmoka.tendermint;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.hotmoka.beans.annotations.Immutable;

/**
 * The configuration of the Tendermint blockchain.
 */
@Immutable
public class Config {

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
	 * The directory where blocks and state will be persisted.
	 * Defaults to {@code chain} in the current directory.
	 */
	public final Path dir;

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
	 * Creates the configuration of a Tendermint blockchain.
	 * 
	 * @param dir the directory where blocks and state will be persisted
	 */
	public Config(Path dir) {
		this.dir = dir;
		this.tendermintPort = 26657;
		this.abciPort = 26658;
		this.maxPingAttempts = 20;
		this.pingDelay = 200;
	}

	/**
	 * Full constructor for the builder pattern.
	 */
	private Config(Path dir, int tendermintPort, int abciPort, int maxPingAttemps, int pingDelay) {
		this.dir = dir;
		this.tendermintPort = tendermintPort;
		this.abciPort = abciPort;
		this.maxPingAttempts = maxPingAttemps;
		this.pingDelay = pingDelay;
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class Builder {
		private int tendermintPort = 26657;
		private int abciPort = 26658;
		private Path dir = Paths.get("chain");
		private int maxPingAttempts = 20;
		private int pingDelay = 200;

		/**
		 * Sets the directory where blocks and state will be persisted.
		 * Defaults to {@code chain} in the current directory.
		 * 
		 * @param dir the directory
		 * @return this builder
		 */
		public Builder setDir(Path dir) {
			this.dir = dir;
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

		/**
		 * Builds the configuration.
		 * 
		 * @return the configuration
		 */
		public Config build() {
			return new Config(dir, tendermintPort, abciPort, maxPingAttempts, pingDelay);
		}
	}
}